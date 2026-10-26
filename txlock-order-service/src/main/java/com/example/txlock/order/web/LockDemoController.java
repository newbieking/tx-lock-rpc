package com.example.txlock.order.web;

import com.example.txlock.order.lock.OrderLockTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo/locks")
@RequiredArgsConstructor
public class LockDemoController {

    private final OrderLockTemplate lockTemplate;

    /**
     * 演示用：占住某个订单锁一段时间，用来观察 tryLock 等待与 LOCK_BUSY。
     * 会阻塞一个请求线程，holdMs 最大 30 秒。
     */
    @PostMapping("/hold")
    public Map<String, Object> hold(@RequestParam String orderNo,
                                    @RequestParam(defaultValue = "3000") long holdMs) {
        long safeHoldMs = Math.max(0, Math.min(holdMs, 30_000));
        boolean acquired = lockTemplate.holdForDemo(orderNo, safeHoldMs);
        return Map.of("orderNo", orderNo, "acquired", acquired, "holdMs", safeHoldMs);
    }

    @GetMapping("/{orderNo}")
    public Map<String, Object> status(@PathVariable String orderNo) {
        return Map.of("orderNo", orderNo, "locked", lockTemplate.isLocked(orderNo));
    }
}
