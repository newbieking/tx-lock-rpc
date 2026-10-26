package com.example.txlock.order.entity;

import com.example.txlock.order.domain.SyncExceptionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SyncExceptionEntity {

    private Long id;
    private String orderNo;
    private String bizNo;
    private String errorType;
    private String errorMsg;
    private Integer attempts;
    private SyncExceptionStatus status;
    private LocalDateTime lastCheckAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
