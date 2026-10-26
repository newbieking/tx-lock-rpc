package com.example.txlock.order.support;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.order.entity.OrderEntity;

import java.time.ZoneId;

public final class RemoteRequests {

    private RemoteRequests() {
    }

    public static RemoteOrderRequest sync(OrderEntity order) {
        return new RemoteOrderRequest(
                "SYNC-" + order.getOrderNo(),
                order.getBizNo(),
                order.getOrderNo(),
                order.getAmount(),
                order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }
}
