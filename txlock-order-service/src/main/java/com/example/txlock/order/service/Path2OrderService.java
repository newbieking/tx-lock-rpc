package com.example.txlock.order.service;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.domain.OutboxStatus;
import com.example.txlock.order.dto.CreateOrderRequest;
import com.example.txlock.order.dto.OrderCreateResponse;
import com.example.txlock.order.entity.OrderEntity;
import com.example.txlock.order.entity.OutboxEventEntity;
import com.example.txlock.order.exception.DuplicateOrderException;
import com.example.txlock.order.lock.OrderLockTemplate;
import com.example.txlock.order.mapper.OrderMapper;
import com.example.txlock.order.mapper.OutboxEventMapper;
import com.example.txlock.order.support.OrderNumbers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Path2OrderService {

    private static final Logger log = LoggerFactory.getLogger(Path2OrderService.class);
    private static final String AGGREGATE_ORDER = "ORDER";

    private final OrderLockTemplate lockTemplate;
    private final OrderMapper orderMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public OrderCreateResponse create(CreateOrderRequest request) {
        String orderNo = OrderNumbers.resolve(request.orderNo());
        String bizNo = "BIZ-" + orderNo;
        String eventId = UUID.randomUUID().toString();
        String payload = toPayload(new RemoteOrderRequest(
                "OUTBOX-" + eventId, bizNo, orderNo, request.amount(), Instant.now()));

        OutboxEventEntity event;
        try {
            // 业务数据和事件记录在同一个本地事务里提交，要么都成功，要么都失败。
            event = lockTemplate.execute(orderNo, () -> transactionTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now();

                OrderEntity order = new OrderEntity();
                order.setOrderNo(orderNo);
                order.setBizNo(bizNo);
                order.setAmount(request.amount());
                order.setStatus(OrderStatus.QUEUED);
                order.setCreatedAt(now);
                order.setUpdatedAt(now);
                orderMapper.insert(order);

                OutboxEventEntity outbox = new OutboxEventEntity();
                outbox.setEventId(eventId);
                outbox.setAggregateType(AGGREGATE_ORDER);
                outbox.setAggregateId(orderNo);
                outbox.setBizNo(bizNo);
                outbox.setPayload(payload);
                outbox.setStatus(OutboxStatus.NEW);
                outbox.setRetryCount(0);
                outbox.setNextRetryAt(now);
                outbox.setCreatedAt(now);
                outbox.setUpdatedAt(now);
                outboxEventMapper.insert(outbox);
                return outbox;
            }));
        } catch (DuplicateKeyException e) {
            throw new DuplicateOrderException(orderNo);
        }

        if (event == null) {
            throw new IllegalStateException("transaction returned no outbox event");
        }
        log.info("path2 order queued orderNo={} eventId={}", orderNo, event.getEventId());
        return new OrderCreateResponse(orderNo, bizNo, OrderStatus.QUEUED, event.getEventId(), 0,
                null, null, "已写入本地消息表，等待发布器异步投递");
    }

    private String toPayload(RemoteOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize outbox payload", e);
        }
    }
}
