package com.example.txlock.order.exception;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderNo) {
        super("order already exists: " + orderNo);
    }
}
