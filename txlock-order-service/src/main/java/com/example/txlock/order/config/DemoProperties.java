package com.example.txlock.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public record DemoProperties(
        Redis redis,
        Remote remote,
        Lock lock,
        Reconcile reconcile,
        Outbox outbox) {

    public record Redis(String address) {
    }

    public record Remote(String baseUrl, int connectTimeoutMs, int readTimeoutMs, int maxAttempts, long backoffMs) {
    }

    public record Lock(long waitMs, long leaseMs) {
    }

    public record Reconcile(String cron, int maxAttempts, int batchSize) {
    }

    public record Outbox(long publishDelayMs, int batchSize, int maxRetry, long retryBackoffMs, long lockMs) {
    }
}
