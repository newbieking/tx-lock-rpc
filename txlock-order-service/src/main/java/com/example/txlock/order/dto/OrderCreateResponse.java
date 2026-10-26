package com.example.txlock.order.dto;

import com.example.txlock.order.domain.OrderStatus;

public record OrderCreateResponse(
        String orderNo,
        String bizNo,
        OrderStatus status,
        String outboxEventId,
        int syncAttempts,
        String errorType,
        String errorMessage,
        String message) {
}
