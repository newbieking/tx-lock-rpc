package com.example.txlock.order.lock;

import com.example.txlock.order.config.DemoProperties;
import com.example.txlock.order.exception.LockBusyException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class OrderLockTemplate {

    public static final String KEY_PREFIX = "txlock:order:";

    private final RedissonClient redissonClient;
    private final DemoProperties properties;

    public OrderLockTemplate(RedissonClient redissonClient, DemoProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    public <T> T execute(String orderNo, Supplier<T> action) {
        RLock lock = redissonClient.getLock(KEY_PREFIX + orderNo);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    properties.lock().waitMs(),
                    properties.lock().leaseMs(),
                    TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockBusyException(orderNo);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockBusyException(orderNo);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public boolean isLocked(String orderNo) {
        return redissonClient.getLock(KEY_PREFIX + orderNo).isLocked();
    }

    public boolean holdForDemo(String orderNo, long holdMs) {
        RLock lock = redissonClient.getLock(KEY_PREFIX + orderNo);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, holdMs + 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!acquired) {
            return false;
        }
        try {
            Thread.sleep(holdMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
