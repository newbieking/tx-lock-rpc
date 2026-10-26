package com.example.txlock.order.entity;

import com.example.txlock.order.domain.OutboxStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OutboxEventEntity {

    private Long id;
    private String eventId;
    private String aggregateType;
    private String aggregateId;
    private String bizNo;
    private String payload;
    private OutboxStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lockedUntil;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
