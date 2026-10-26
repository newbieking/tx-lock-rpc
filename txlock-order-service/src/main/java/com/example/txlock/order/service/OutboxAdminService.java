package com.example.txlock.order.service;

import com.example.txlock.order.dto.OutboxEventView;
import com.example.txlock.order.entity.OutboxEventEntity;
import com.example.txlock.order.exception.RequeueNotAllowedException;
import com.example.txlock.order.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxAdminService {

    private final OutboxEventMapper outboxEventMapper;

    public List<OutboxEventView> recent(int limit) {
        return outboxEventMapper.findRecent(clamp(limit)).stream()
                .map(OutboxEventView::from)
                .toList();
    }

    public OutboxEventView requeue(long id) {
        int updated = outboxEventMapper.requeue(id, LocalDateTime.now());
        if (updated == 0) {
            throw new RequeueNotAllowedException(id);
        }
        OutboxEventEntity event = outboxEventMapper.findById(id);
        if (event == null) {
            throw new RequeueNotAllowedException(id);
        }
        return OutboxEventView.from(event);
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, 200));
    }
}
