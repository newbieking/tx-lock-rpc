package com.example.txlock.order.web;

import com.example.txlock.order.dto.CreateOrderRequest;
import com.example.txlock.order.dto.OrderCreateResponse;
import com.example.txlock.order.dto.OutboxEventView;
import com.example.txlock.order.dto.PublishSummary;
import com.example.txlock.order.service.OutboxAdminService;
import com.example.txlock.order.service.OutboxPublisher;
import com.example.txlock.order.service.Path2OrderService;
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
@RequestMapping("/demo/path2")
@RequiredArgsConstructor
public class Path2Controller {

    private final Path2OrderService path2OrderService;
    private final OutboxPublisher outboxPublisher;
    private final OutboxAdminService outboxAdminService;

    @PostMapping("/orders")
    public OrderCreateResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return path2OrderService.create(request);
    }

    @PostMapping("/publish")
    public PublishSummary publish() {
        return outboxPublisher.publishBatch();
    }

    @GetMapping("/outbox")
    public List<OutboxEventView> outbox(@RequestParam(defaultValue = "50") int limit) {
        return outboxAdminService.recent(limit);
    }

    @PostMapping("/outbox/{id}/requeue")
    public OutboxEventView requeue(@PathVariable long id) {
        return outboxAdminService.requeue(id);
    }
}
