package com.example.txlock.order.service;

import com.example.txlock.order.domain.SyncExceptionStatus;
import com.example.txlock.order.entity.OrderEntity;
import com.example.txlock.order.entity.SyncExceptionEntity;
import com.example.txlock.order.mapper.SyncExceptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SyncExceptionRecorder {

    private final SyncExceptionMapper syncExceptionMapper;

    /**
     * 必须在调用方的事务内执行，让异常记录与订单状态一起提交。
     * 返回该订单累计的失败尝试次数。
     */
    public int recordFailure(OrderEntity order, String errorType, String errorMessage, int addAttempts) {
        LocalDateTime now = LocalDateTime.now();
        int increment = Math.max(0, addAttempts);

        SyncExceptionEntity open = syncExceptionMapper.findPendingByOrderNo(order.getOrderNo());
        if (open == null) {
            SyncExceptionEntity entity = new SyncExceptionEntity();
            entity.setOrderNo(order.getOrderNo());
            entity.setBizNo(order.getBizNo());
            entity.setErrorType(errorType);
            entity.setErrorMsg(errorMessage);
            entity.setAttempts(increment);
            entity.setStatus(SyncExceptionStatus.PENDING);
            entity.setLastCheckAt(now);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            syncExceptionMapper.insert(entity);
            return increment;
        }

        int total = open.getAttempts() + increment;
        open.setErrorType(errorType);
        open.setErrorMsg(errorMessage);
        open.setAttempts(total);
        open.setLastCheckAt(now);
        open.setUpdatedAt(now);
        syncExceptionMapper.update(open);
        return total;
    }
}
