package com.example.txlock.common;

import java.math.BigDecimal;
import java.time.Instant;

public record RemoteOrderView(
        String remoteNo,
        String bizNo,
        String orderNo,
        BigDecimal amount,
        String eventId,
        Instant receivedAt) {
}
