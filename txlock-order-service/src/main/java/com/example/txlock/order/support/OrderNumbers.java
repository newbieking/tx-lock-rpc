package com.example.txlock.order.support;

import java.util.UUID;

public final class OrderNumbers {

    private OrderNumbers() {
    }

    public static String resolve(String orderNo) {
        return orderNo == null || orderNo.isBlank() ? "ORD-" + UUID.randomUUID() : orderNo.trim();
    }
}
