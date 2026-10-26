package com.example.txlock.order.web;

import com.example.txlock.order.dto.OrderView;
import com.example.txlock.order.service.OrderQueryService;
import com.example.txlock.order.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final OrderQueryService orderQueryService;
    private final StatsService statsService;

    @GetMapping("/orders")
    public List<OrderView> orders(@RequestParam(defaultValue = "20") int limit) {
        return orderQueryService.recent(limit);
    }

    @GetMapping("/orders/{orderNo}")
    public OrderView order(@PathVariable String orderNo) {
        return orderQueryService.get(orderNo);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return statsService.stats();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        statsService.reset();
        return Map.of("reset", true);
    }
}
