package com.example.txlock.order.mapper;

import com.example.txlock.order.entity.SyncExceptionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SyncExceptionMapper {

    @Insert("""
            INSERT INTO t_sync_exception
                (order_no, biz_no, error_type, error_msg, attempts, status, last_check_at, created_at, updated_at)
            VALUES
                (#{orderNo}, #{bizNo}, #{errorType}, #{errorMsg}, #{attempts}, #{status}, #{lastCheckAt}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SyncExceptionEntity exception);

    @Select("""
            SELECT * FROM t_sync_exception
            WHERE order_no = #{orderNo} AND status = 'PENDING'
            ORDER BY id DESC
            LIMIT 1
            """)
    SyncExceptionEntity findPendingByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM t_sync_exception WHERE status = 'PENDING' ORDER BY id LIMIT #{limit}")
    List<SyncExceptionEntity> findPending(@Param("limit") int limit);

    @Select("SELECT * FROM t_sync_exception ORDER BY id DESC LIMIT #{limit}")
    List<SyncExceptionEntity> findRecent(@Param("limit") int limit);

    @Update("""
            UPDATE t_sync_exception
            SET error_type = #{errorType}, error_msg = #{errorMsg}, attempts = #{attempts},
                status = #{status}, last_check_at = #{lastCheckAt}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(SyncExceptionEntity exception);

    @Update("""
            UPDATE t_sync_exception
            SET status = 'RESOLVED', last_check_at = #{now}, updated_at = #{now}
            WHERE order_no = #{orderNo} AND status IN ('PENDING', 'MANUAL_REQUIRED')
            """)
    int resolveByOrderNo(@Param("orderNo") String orderNo, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE t_sync_exception
            SET status = 'MANUAL_REQUIRED', last_check_at = #{now}, updated_at = #{now}
            WHERE order_no = #{orderNo} AND status = 'PENDING'
            """)
    int markManualByOrderNo(@Param("orderNo") String orderNo, @Param("now") LocalDateTime now);
}
