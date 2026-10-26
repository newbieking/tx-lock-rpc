package com.example.txlock.order.service;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.order.client.RemoteCallResult;
import com.example.txlock.order.client.RemoteCaller;
import com.example.txlock.order.config.DemoProperties;
import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.dto.CreateOrderRequest;
import com.example.txlock.order.dto.OrderCreateResponse;
import com.example.txlock.order.entity.OrderEntity;
import com.example.txlock.order.exception.DuplicateOrderException;
import com.example.txlock.order.exception.OrderNotFoundException;
import com.example.txlock.order.lock.OrderLockTemplate;
import com.example.txlock.order.mapper.OrderMapper;
import com.example.txlock.order.mapper.SyncExceptionMapper;
import com.example.txlock.order.support.OrderNumbers;
import com.example.txlock.order.support.RemoteRequests;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class Path1OrderService {

    private static final Logger log = LoggerFactory.getLogger(Path1OrderService.class);

    private final OrderLockTemplate lockTemplate;
    private final OrderMapper orderMapper;
    private final SyncExceptionMapper syncExceptionMapper;
    private final SyncExceptionRecorder syncExceptionRecorder;
    private final RemoteCaller remoteCaller;
    private final TransactionTemplate transactionTemplate;
    private final DemoProperties properties;

    public OrderCreateResponse create(CreateOrderRequest request) {
        String orderNo = OrderNumbers.resolve(request.orderNo());
        String bizNo = "BIZ-" + orderNo;

        // 锁和本地事务只覆盖本地写入；方法返回时事务已提交、锁已释放。
        OrderEntity order = lockTemplate.execute(orderNo, () -> insertLocal(orderNo, bizNo, request.amount()));

        // 事务外调用远程接口，失败做有限次同步重试。
        RemoteOrderRequest remoteRequest = RemoteRequests.sync(order);
        RemoteCallResult result = remoteCaller.callWithRetry(remoteRequest, properties.remote().maxAttempts());

        if (result.success()) {
            transactionTemplate.executeWithoutResult(status ->
                    orderMapper.updateStatus(orderNo, OrderStatus.SYNCED, LocalDateTime.now()));
            log.info("path1 order synced orderNo={} attempts={} duplicate={}",
                    orderNo, result.attempts(), result.duplicate());
            return new OrderCreateResponse(orderNo, bizNo, OrderStatus.SYNCED, null,
                    result.attempts(), null, null, "本地事务已提交，远程同步成功");
        }

        transactionTemplate.executeWithoutResult(status -> {
            orderMapper.updateStatus(orderNo, OrderStatus.SYNC_FAILED, LocalDateTime.now());
            syncExceptionRecorder.recordFailure(order, result.errorType(), result.errorMessage(), result.attempts());
        });
        log.error("[ALERT] path1 sync failed orderNo={} attempts={} type={} error={}",
                orderNo, result.attempts(), result.errorType(), result.errorMessage());
        return new OrderCreateResponse(orderNo, bizNo, OrderStatus.SYNC_FAILED, null,
                result.attempts(), result.errorType(), result.errorMessage(),
                "本地事务已提交，远程同步失败，已记录待处理异常");
    }

    public OrderCreateResponse resync(String orderNo) {
        OrderEntity order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new OrderNotFoundException(orderNo);
        }
        if (order.getStatus() == OrderStatus.SYNCED) {
            return new OrderCreateResponse(orderNo, order.getBizNo(), OrderStatus.SYNCED, null, 0,
                    null, null, "订单已同步，无需人工补偿");
        }

        RemoteCallResult result = remoteCaller.callWithRetry(
                RemoteRequests.sync(order), properties.remote().maxAttempts());

        if (result.success()) {
            transactionTemplate.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                orderMapper.updateStatus(orderNo, OrderStatus.SYNCED, now);
                syncExceptionMapper.resolveByOrderNo(orderNo, now);
            });
            log.info("path1 manual resync succeeded orderNo={} attempts={}", orderNo, result.attempts());
            return new OrderCreateResponse(orderNo, order.getBizNo(), OrderStatus.SYNCED, null,
                    result.attempts(), null, null, "人工补偿成功，本地与远程已对齐");
        }

        OrderStatus finalStatus = order.getStatus() == OrderStatus.MANUAL
                ? OrderStatus.MANUAL : OrderStatus.SYNC_FAILED;
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            orderMapper.updateStatus(orderNo, finalStatus, now);
            syncExceptionRecorder.recordFailure(order, result.errorType(), result.errorMessage(), result.attempts());
            if (finalStatus == OrderStatus.MANUAL) {
                syncExceptionMapper.markManualByOrderNo(orderNo, now);
            }
        });
        log.error("[ALERT] path1 manual resync failed orderNo={} attempts={} type={} error={}",
                orderNo, result.attempts(), result.errorType(), result.errorMessage());
        return new OrderCreateResponse(orderNo, order.getBizNo(), finalStatus, null,
                result.attempts(), result.errorType(), result.errorMessage(), "人工补偿仍失败，异常记录已更新");
    }

    private OrderEntity insertLocal(String orderNo, String bizNo, BigDecimal amount) {
        try {
            OrderEntity order = transactionTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now();
                OrderEntity entity = new OrderEntity();
                entity.setOrderNo(orderNo);
                entity.setBizNo(bizNo);
                entity.setAmount(amount);
                entity.setStatus(OrderStatus.PENDING_SYNC);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                orderMapper.insert(entity);
                return entity;
            });
            if (order == null) {
                throw new IllegalStateException("transaction returned no order");
            }
            return order;
        } catch (DuplicateKeyException e) {
            throw new DuplicateOrderException(orderNo);
        }
    }
}
