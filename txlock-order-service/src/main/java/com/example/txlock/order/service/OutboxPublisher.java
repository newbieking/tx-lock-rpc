package com.example.txlock.order.service;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.order.client.RemoteCallResult;
import com.example.txlock.order.client.RemoteCaller;
import com.example.txlock.order.config.DemoProperties;
import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.domain.OutboxStatus;
import com.example.txlock.order.dto.PublishSummary;
import com.example.txlock.order.entity.OutboxEventEntity;
import com.example.txlock.order.mapper.OrderMapper;
import com.example.txlock.order.mapper.OutboxEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventMapper outboxEventMapper;
    private final OrderMapper orderMapper;
    private final RemoteCaller remoteCaller;
    private final TransactionTemplate transactionTemplate;
    private final DemoProperties properties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${demo.outbox.publish-delay-ms:1000}")
    public void scheduledPublish() {
        try {
            PublishSummary summary = publishBatch();
            if (summary.due() > 0 || summary.recovered() > 0) {
                log.info("outbox publish batch {}", summary);
            }
        } catch (RuntimeException e) {
            log.error("outbox publish batch failed", e);
        }
    }

    /**
     * 同一实例内串行执行；多实例部署时靠 claim 的 CAS 更新保证同一个事件只被一个发布器领取。
     */
    public synchronized PublishSummary publishBatch() {
        LocalDateTime now = LocalDateTime.now();
        int recovered = outboxEventMapper.recoverStale(now);
        List<OutboxEventEntity> due = outboxEventMapper.findDue(now, properties.outbox().batchSize());

        int sent = 0;
        int retried = 0;
        int dead = 0;
        int skipped = 0;

        for (OutboxEventEntity event : due) {
            LocalDateTime claimedAt = LocalDateTime.now();
            LocalDateTime lockedUntil = claimedAt.plusNanos(properties.outbox().lockMs() * 1_000_000L);
            if (outboxEventMapper.claim(event.getId(), lockedUntil, claimedAt) == 0) {
                skipped++;
                continue;
            }

            RemoteCallResult result = remoteCaller.callWithRetry(
                    readPayload(event), properties.remote().maxAttempts());

            if (result.success()) {
                markSent(event);
                sent++;
                log.info("outbox event sent eventId={} bizNo={} attempts={} duplicate={}",
                        event.getEventId(), event.getBizNo(), result.attempts(), result.duplicate());
                continue;
            }

            if (handleFailure(event, result)) {
                dead++;
            } else {
                retried++;
            }
        }

        return new PublishSummary(recovered, due.size(), sent, retried, dead, skipped);
    }

    private void markSent(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            outboxEventMapper.markSent(event.getId(), now);
            orderMapper.updateStatus(event.getAggregateId(), OrderStatus.SYNCED, now);
        });
    }

    private boolean handleFailure(OutboxEventEntity event, RemoteCallResult result) {
        int retryCount = event.getRetryCount() + 1;
        boolean dead = retryCount >= properties.outbox().maxRetry();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRetryAt = now.plusNanos(
                properties.outbox().retryBackoffMs() * retryCount * 1_000_000L);

        outboxEventMapper.markAttemptFailed(
                event.getId(),
                dead ? OutboxStatus.DEAD : OutboxStatus.NEW,
                retryCount,
                nextRetryAt,
                result.errorMessage(),
                now);

        if (dead) {
            log.error("[ALERT] outbox event exhausted retries eventId={} bizNo={} retries={} error={}",
                    event.getEventId(), event.getBizNo(), retryCount, result.errorMessage());
            return true;
        }
        log.warn("outbox event retry scheduled eventId={} retry={} nextRetryAt={} error={}",
                event.getEventId(), retryCount, nextRetryAt, result.errorMessage());
        return false;
    }

    private RemoteOrderRequest readPayload(OutboxEventEntity event) {
        try {
            return objectMapper.readValue(event.getPayload(), RemoteOrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("invalid outbox payload for event " + event.getEventId(), e);
        }
    }
}
