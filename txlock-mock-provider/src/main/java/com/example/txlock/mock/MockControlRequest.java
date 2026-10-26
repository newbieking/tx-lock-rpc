package com.example.txlock.mock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MockControlRequest(
        @NotNull MockMode mode,
        @Min(-1) Integer times,
        @Min(0) Long delayMs) {
}
