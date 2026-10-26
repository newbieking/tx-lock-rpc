package com.example.txlock.mock;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.common.RemoteOrderView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MockState {

    private final AtomicReference<MockMode> mode = new AtomicReference<>(MockMode.SUCCESS);
    private final AtomicInteger remaining = new AtomicInteger(-1);
    private final AtomicLong delayMs = new AtomicLong(0);

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong timeouts = new AtomicLong();
    private final AtomicLong sequence = new AtomicLong();

    private final Map<String, RemoteOrderView> orders = new ConcurrentHashMap<>();

    public record StoreResult(RemoteOrderView view, boolean duplicate) {
    }

    public synchronized void configure(MockMode mode, Integer times, Long delayMs) {
        this.mode.set(mode);
        this.remaining.set(times != null && times > 0 ? times : -1);
        this.delayMs.set(delayMs == null ? 0 : delayMs);
    }

    /**
     * times > 0 时只影响接下来的 N 次请求；times <= 0 或缺省时一直生效，直到重新设置。
     */
    public synchronized MockMode nextDecision() {
        MockMode current = mode.get();
        if (current == MockMode.SUCCESS) {
            return current;
        }
        int left = remaining.get();
        if (left < 0) {
            return current;
        }
        if (left == 0) {
            mode.set(MockMode.SUCCESS);
            return MockMode.SUCCESS;
        }
        if (left == 1) {
            mode.set(MockMode.SUCCESS);
            remaining.set(-1);
        } else {
            remaining.set(left - 1);
        }
        return current;
    }

    public long delayFor(MockMode decision) {
        long configured = delayMs.get();
        if (configured > 0) {
            return configured;
        }
        return switch (decision) {
            case TIMEOUT -> 5000L;
            case SLOW -> 300L;
            default -> 0L;
        };
    }

    public StoreResult store(RemoteOrderRequest request) {
        RemoteOrderView candidate = new RemoteOrderView(
                "RNO-" + sequence.incrementAndGet(),
                request.bizNo(),
                request.orderNo(),
                request.amount(),
                request.eventId(),
                Instant.now());
        RemoteOrderView previous = orders.putIfAbsent(request.bizNo(), candidate);
        if (previous != null) {
            duplicates.incrementAndGet();
            return new StoreResult(previous, true);
        }
        accepted.incrementAndGet();
        return new StoreResult(candidate, false);
    }

    public RemoteOrderView find(String bizNo) {
        return orders.get(bizNo);
    }

    public List<RemoteOrderView> allOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(RemoteOrderView::receivedAt))
                .toList();
    }

    public void recordRequest() {
        totalRequests.incrementAndGet();
    }

    public void recordFailed() {
        failed.incrementAndGet();
    }

    public void recordRejected() {
        rejected.incrementAndGet();
    }

    public void recordTimeout() {
        timeouts.incrementAndGet();
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mode", mode.get());
        snapshot.put("remaining", remaining.get());
        snapshot.put("delayMs", delayMs.get());
        snapshot.put("totalRequests", totalRequests.get());
        snapshot.put("accepted", accepted.get());
        snapshot.put("duplicates", duplicates.get());
        snapshot.put("failed", failed.get());
        snapshot.put("rejected", rejected.get());
        snapshot.put("timeouts", timeouts.get());
        snapshot.put("storedOrders", orders.size());
        return snapshot;
    }

    public synchronized void reset() {
        mode.set(MockMode.SUCCESS);
        remaining.set(-1);
        delayMs.set(0);
        totalRequests.set(0);
        accepted.set(0);
        duplicates.set(0);
        failed.set(0);
        rejected.set(0);
        timeouts.set(0);
        sequence.set(0);
        orders.clear();
    }
}
