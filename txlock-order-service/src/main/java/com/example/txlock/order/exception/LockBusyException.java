package com.example.txlock.order.exception;

public class LockBusyException extends RuntimeException {

    public LockBusyException(String orderNo) {
        super("order " + orderNo + " is being processed by another request");
    }
}
