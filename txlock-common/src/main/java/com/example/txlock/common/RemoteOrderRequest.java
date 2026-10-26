package com.example.txlock.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record RemoteOrderRequest(
        @NotBlank String eventId,
        @NotBlank String bizNo,
        @NotBlank String orderNo,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull Instant occurredAt) {
}
