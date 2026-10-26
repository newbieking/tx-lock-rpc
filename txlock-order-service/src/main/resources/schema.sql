CREATE TABLE IF NOT EXISTS t_order (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no   VARCHAR(64)    NOT NULL,
    biz_no     VARCHAR(128)   NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL,
    status     VARCHAR(32)    NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    CONSTRAINT uk_order_no UNIQUE (order_no),
    CONSTRAINT uk_order_biz_no UNIQUE (biz_no)
);

CREATE TABLE IF NOT EXISTS t_sync_exception (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(64)  NOT NULL,
    biz_no        VARCHAR(128) NOT NULL,
    error_type    VARCHAR(32)  NOT NULL,
    error_msg     VARCHAR(500),
    attempts      INT          NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    last_check_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sync_exception_status ON t_sync_exception (status);
CREATE INDEX IF NOT EXISTS idx_sync_exception_order ON t_sync_exception (order_no, status);

CREATE TABLE IF NOT EXISTS t_outbox_event (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id       VARCHAR(64)   NOT NULL,
    aggregate_type VARCHAR(64)   NOT NULL,
    aggregate_id   VARCHAR(64)   NOT NULL,
    biz_no         VARCHAR(128)  NOT NULL,
    payload        VARCHAR(4000) NOT NULL,
    status         VARCHAR(32)   NOT NULL,
    retry_count    INT           NOT NULL,
    next_retry_at  TIMESTAMP     NOT NULL,
    locked_until   TIMESTAMP,
    last_error     VARCHAR(500),
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_due ON t_outbox_event (status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON t_outbox_event (aggregate_id);
