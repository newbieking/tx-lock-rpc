package com.example.txlock.order.client;

import com.example.txlock.common.RemoteOrderView;

public record RemoteLookup(Status status, RemoteOrderView view, String errorMessage) {

    public enum Status {
        FOUND,
        NOT_FOUND,
        ERROR
    }

    public static RemoteLookup found(RemoteOrderView view) {
        return new RemoteLookup(Status.FOUND, view, null);
    }

    public static RemoteLookup notFound() {
        return new RemoteLookup(Status.NOT_FOUND, null, null);
    }

    public static RemoteLookup error(String message) {
        return new RemoteLookup(Status.ERROR, null, message);
    }
}
