package com.example.txlock.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderNo) {
        super("order not found: " + orderNo);
    }
}
