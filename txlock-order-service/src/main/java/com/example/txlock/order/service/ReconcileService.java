package com.example.txlock.order.service;

import com.example.txlock.order.client.RemoteCallResult;
import com.example.txlock.order.client.RemoteCaller;
import com.example.txlock.order.client.RemoteLookup;
import com.example.txlock.order.config.DemoProperties;
import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.dto.ReconcileSummary;
import com.example.txlock.order.entity.OrderEntity;
import com.example.txlock.order.entity.SyncExceptionEntity;
import com.example.txlock.order.mapper.OrderMapper;
import com.example.txlock.order.mapper.SyncExceptionMapper;
import com.example.txlock.order.support.RemoteRequests;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileService.class);

    private final OrderMapper orderMapper;
    private final SyncExceptionMapper syncExceptionMapper;
    private final SyncExceptionRecorder syncExceptionRecorder;
    private final RemoteCaller remoteCaller;
    private final TransactionTemplate transactionTemplate;
    private final DemoProperties properties;

    @Scheduled(cron = "${demo.reconcile.cron:0 0 2 * * *}")
    public void scheduledReconcile() {
        try {
            log.info("scheduled reconcile result {}", reconcile());
        } catch (RuntimeException e) {
            log.error("scheduled reconcile failed", e);
        }
    }

    /**
     * 以本地未对齐订单和待处理异常为入口，逐个查询远端：
     * 远端已有 -> 直接对齐；远端没有 -> 只做一次补偿调用，避免对账任务放大故障。
     */
    public ReconcileSummary reconcile() {
        long startedAt = System.currentTimeMillis();
        Map<String, OrderEntity> targets = new LinkedHashMap<>();

        for (OrderEntity order : orderMapper.findUnsettled(properties.reconcile().batchSize())) {
            targets.put(order.getOrderNo(), order);
        }
        for (SyncExceptionEntity exception : syncExceptionMapper.findPending(properties.reconcile().batchSize())) {
            if (!targets.containsKey(exception.getOrderNo())) {
                OrderEntity order = orderMapper.findByOrderNo(exception.getOrderNo());
                if (order != null) {
                    targets.put(order.getOrderNo(), order);
                }
            }
        }

        int checked = 0;
        int remoteFound = 0;
        int compensated = 0;
        int stillPending = 0;
        int escalated = 0;
        int skipped = 0;

        for (OrderEntity order : targets.values()) {
            checked++;
            RemoteLookup lookup = remoteCaller.lookup(order.getBizNo());

            if (lookup.status() == RemoteLookup.Status.FOUND) {
                resolve(order.getOrderNo());
                remoteFound++;
                continue;
            }

            if (lookup.status() == RemoteLookup.Status.ERROR) {
                if (order.getStatus() != OrderStatus.MANUAL) {
                    transactionTemplate.executeWithoutResult(status -> {
                        orderMapper.updateStatus(order.getOrderNo(), OrderStatus.SYNC_FAILED, LocalDateTime.now());
                        syncExceptionRecorder.recordFailure(order, "LOOKUP_ERROR", lookup.errorMessage(), 0);
                    });
                }
                skipped++;
                continue;
            }

            if (order.getStatus() == OrderStatus.MANUAL) {
                skipped++;
                continue;
            }

            RemoteCallResult result = remoteCaller.callWithRetry(RemoteRequests.sync(order), 1);
            if (result.success()) {
                resolve(order.getOrderNo());
                compensated++;
                continue;
            }

            int totalAttempts = transactionTemplate.execute(status -> {
                orderMapper.updateStatus(order.getOrderNo(), OrderStatus.SYNC_FAILED, LocalDateTime.now());
                return syncExceptionRecorder.recordFailure(
                        order, result.errorType(), result.errorMessage(), result.attempts());
            });
            if (totalAttempts >= properties.reconcile().maxAttempts()) {
                escalate(order.getOrderNo());
                escalated++;
            } else {
                stillPending++;
            }
        }

        ReconcileSummary summary = new ReconcileSummary(
                checked, remoteFound, compensated, stillPending, escalated, skipped,
                System.currentTimeMillis() - startedAt);
        log.info("reconcile finished {}", summary);
        return summary;
    }

    private void resolve(String orderNo) {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            orderMapper.updateStatus(orderNo, OrderStatus.SYNCED, now);
            syncExceptionMapper.resolveByOrderNo(orderNo, now);
        });
    }

    private void escalate(String orderNo) {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            orderMapper.updateStatus(orderNo, OrderStatus.MANUAL, now);
            syncExceptionMapper.markManualByOrderNo(orderNo, now);
        });
        log.error("[ALERT] order escalated to MANUAL orderNo={}", orderNo);
    }
}
