package com.example.txlock.order.dto;

public record ReconcileSummary(
        int checked,
        int remoteFound,
        int compensated,
        int stillPending,
        int escalated,
        int skipped,
        long elapsedMs) {
}
