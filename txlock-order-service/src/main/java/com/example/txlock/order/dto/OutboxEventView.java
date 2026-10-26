package com.example.txlock.order.dto;

import com.example.txlock.order.domain.OutboxStatus;
import com.example.txlock.order.entity.OutboxEventEntity;

import java.time.LocalDateTime;

public record OutboxEventView(
        Long id,
        String eventId,
        String aggregateId,
        String bizNo,
        String payload,
        OutboxStatus status,
        int retryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lockedUntil,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static OutboxEventView from(OutboxEventEntity event) {
        return new OutboxEventView(
                event.getId(),
                event.getEventId(),
                event.getAggregateId(),
                event.getBizNo(),
                event.getPayload(),
                event.getStatus(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getLockedUntil(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
