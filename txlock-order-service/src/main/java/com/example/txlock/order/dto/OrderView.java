package com.example.txlock.order.dto;

import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.entity.OrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderView(
        String orderNo,
        String bizNo,
        BigDecimal amount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static OrderView from(OrderEntity order) {
        return new OrderView(
                order.getOrderNo(),
                order.getBizNo(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
