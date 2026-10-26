package com.example.txlock.order.service;

import com.example.txlock.order.dto.OrderView;
import com.example.txlock.order.entity.OrderEntity;
import com.example.txlock.order.entity.SyncExceptionEntity;
import com.example.txlock.order.exception.OrderNotFoundException;
import com.example.txlock.order.mapper.OrderMapper;
import com.example.txlock.order.mapper.SyncExceptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderMapper orderMapper;
    private final SyncExceptionMapper syncExceptionMapper;

    public List<OrderView> recent(int limit) {
        return orderMapper.findRecent(clamp(limit)).stream()
                .map(OrderView::from)
                .toList();
    }

    public OrderView get(String orderNo) {
        OrderEntity order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new OrderNotFoundException(orderNo);
        }
        return OrderView.from(order);
    }

    public List<SyncExceptionEntity> recentExceptions(int limit) {
        return syncExceptionMapper.findRecent(clamp(limit));
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, 200));
    }
}
