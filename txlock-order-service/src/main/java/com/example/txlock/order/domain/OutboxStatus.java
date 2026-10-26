package com.example.txlock.order.domain;

public enum OutboxStatus {
    NEW,
    SENDING,
    SENT,
    DEAD
}
