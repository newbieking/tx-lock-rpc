package com.example.txlock.order.dto;

public record PublishSummary(
        int recovered,
        int due,
        int sent,
        int retried,
        int dead,
        int skipped) {
}
