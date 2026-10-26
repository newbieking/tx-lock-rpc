package com.example.txlock.order.domain;

public enum OrderStatus {
    PENDING_SYNC,
    QUEUED,
    SYNCED,
    SYNC_FAILED,
    MANUAL
}
