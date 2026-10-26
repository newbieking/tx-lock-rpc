package com.example.txlock.order.client;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.common.RemoteOrderResponse;
import com.example.txlock.common.RemoteOrderView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface RemoteOrderApi {

    @PostExchange("/api/remote/orders")
    RemoteOrderResponse sync(@RequestBody RemoteOrderRequest request);

    @GetExchange("/api/remote/orders/{bizNo}")
    RemoteOrderView get(@PathVariable("bizNo") String bizNo);
}
