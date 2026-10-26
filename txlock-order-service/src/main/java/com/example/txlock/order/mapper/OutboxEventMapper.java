package com.example.txlock.order.mapper;

import com.example.txlock.order.domain.OutboxStatus;
import com.example.txlock.order.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper {

    @Insert("""
            INSERT INTO t_outbox_event
                (event_id, aggregate_type, aggregate_id, biz_no, payload, status,
                 retry_count, next_retry_at, locked_until, last_error, created_at, updated_at)
            VALUES
                (#{eventId}, #{aggregateType}, #{aggregateId}, #{bizNo}, #{payload}, #{status},
                 #{retryCount}, #{nextRetryAt}, #{lockedUntil}, #{lastError}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OutboxEventEntity event);

    @Select("""
            SELECT * FROM t_outbox_event
            WHERE status = 'NEW' AND next_retry_at <= #{now}
            ORDER BY id
            LIMIT #{limit}
            """)
    List<OutboxEventEntity> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'SENDING', locked_until = #{lockedUntil}, updated_at = #{now}
            WHERE id = #{id} AND status = 'NEW'
            """)
    int claim(@Param("id") Long id,
              @Param("lockedUntil") LocalDateTime lockedUntil,
              @Param("now") LocalDateTime now);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'NEW', locked_until = NULL, next_retry_at = #{now}, updated_at = #{now}
            WHERE status = 'SENDING' AND locked_until < #{now}
            """)
    int recoverStale(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'SENT', last_error = NULL, locked_until = NULL, updated_at = #{now}
            WHERE id = #{id}
            """)
    int markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE t_outbox_event
            SET status = #{status}, retry_count = #{retryCount}, next_retry_at = #{nextRetryAt},
                last_error = #{lastError}, locked_until = NULL, updated_at = #{now}
            WHERE id = #{id}
            """)
    int markAttemptFailed(@Param("id") Long id,
                          @Param("status") OutboxStatus status,
                          @Param("retryCount") int retryCount,
                          @Param("nextRetryAt") LocalDateTime nextRetryAt,
                          @Param("lastError") String lastError,
                          @Param("now") LocalDateTime now);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'NEW', retry_count = 0, next_retry_at = #{now},
                last_error = NULL, locked_until = NULL, updated_at = #{now}
            WHERE id = #{id} AND status = 'DEAD'
            """)
    int requeue(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM t_outbox_event WHERE id = #{id}")
    OutboxEventEntity findById(@Param("id") Long id);

    @Select("SELECT * FROM t_outbox_event ORDER BY id DESC LIMIT #{limit}")
    List<OutboxEventEntity> findRecent(@Param("limit") int limit);
}
