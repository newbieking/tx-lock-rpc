package com.example.txlock.common;

import java.time.Instant;

public record RemoteOrderResponse(
        String remoteNo,
        String bizNo,
        boolean duplicate,
        Instant receivedAt) {
}
