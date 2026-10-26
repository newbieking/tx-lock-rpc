package com.example.txlock.order.exception;

public class RequeueNotAllowedException extends RuntimeException {

    public RequeueNotAllowedException(long eventId) {
        super("outbox event " + eventId + " does not exist or is not in DEAD status");
    }
}
