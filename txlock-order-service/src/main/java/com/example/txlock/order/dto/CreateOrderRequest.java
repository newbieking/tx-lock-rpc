package com.example.txlock.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @Pattern(regexp = "^$|^[A-Za-z0-9_-]{1,64}$",
                message = "must be 1-64 chars of letters, digits, underscore or hyphen")
        String orderNo,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2)
        BigDecimal amount) {
}
