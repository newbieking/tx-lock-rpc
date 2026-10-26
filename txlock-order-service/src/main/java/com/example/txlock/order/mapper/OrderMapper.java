package com.example.txlock.order.mapper;

import com.example.txlock.order.domain.OrderStatus;
import com.example.txlock.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
            INSERT INTO t_order (order_no, biz_no, amount, status, created_at, updated_at)
            VALUES (#{orderNo}, #{bizNo}, #{amount}, #{status}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderEntity order);

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    OrderEntity findByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM t_order ORDER BY id DESC LIMIT #{limit}")
    List<OrderEntity> findRecent(@Param("limit") int limit);

    @Select("""
            SELECT * FROM t_order
            WHERE status IN ('PENDING_SYNC', 'SYNC_FAILED', 'MANUAL')
            ORDER BY id
            LIMIT #{limit}
            """)
    List<OrderEntity> findUnsettled(@Param("limit") int limit);

    @Update("UPDATE t_order SET status = #{status}, updated_at = #{updatedAt} WHERE order_no = #{orderNo}")
    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("status") OrderStatus status,
                     @Param("updatedAt") LocalDateTime updatedAt);
}
