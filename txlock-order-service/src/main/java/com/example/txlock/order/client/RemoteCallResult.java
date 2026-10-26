package com.example.txlock.order.client;

public record RemoteCallResult(
        boolean success,
        int attempts,
        String errorType,
        String errorMessage,
        boolean duplicate) {

    public static RemoteCallResult success(int attempts, boolean duplicate) {
        return new RemoteCallResult(true, attempts, null, null, duplicate);
    }

    public static RemoteCallResult failure(int attempts, String errorType, String errorMessage) {
        return new RemoteCallResult(false, attempts, errorType, errorMessage, false);
    }
}
