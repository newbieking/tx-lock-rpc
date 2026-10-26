package com.example.txlock.order.web;

import com.example.txlock.order.dto.CreateOrderRequest;
import com.example.txlock.order.dto.OrderCreateResponse;
import com.example.txlock.order.dto.ReconcileSummary;
import com.example.txlock.order.entity.SyncExceptionEntity;
import com.example.txlock.order.service.OrderQueryService;
import com.example.txlock.order.service.Path1OrderService;
import com.example.txlock.order.service.ReconcileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo/path1")
@RequiredArgsConstructor
public class Path1Controller {

    private final Path1OrderService path1OrderService;
    private final ReconcileService reconcileService;
    private final OrderQueryService orderQueryService;

    @PostMapping("/orders")
    public OrderCreateResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return path1OrderService.create(request);
    }

    @PostMapping("/orders/{orderNo}/resync")
    public OrderCreateResponse resync(@PathVariable String orderNo) {
        return path1OrderService.resync(orderNo);
    }

    @PostMapping("/reconcile")
    public ReconcileSummary reconcile() {
        return reconcileService.reconcile();
    }

    @GetMapping("/exceptions")
    public List<SyncExceptionEntity> exceptions(@RequestParam(defaultValue = "50") int limit) {
        return orderQueryService.recentExceptions(limit);
    }
}
