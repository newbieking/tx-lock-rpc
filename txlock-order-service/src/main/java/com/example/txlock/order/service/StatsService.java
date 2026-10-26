package com.example.txlock.order.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatsService {

    private final JdbcTemplate jdbcTemplate;

    public StatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ordersByStatus",
                jdbcTemplate.queryForList("SELECT status, COUNT(*) AS cnt FROM t_order GROUP BY status ORDER BY status"));
        result.put("outboxByStatus",
                jdbcTemplate.queryForList("SELECT status, COUNT(*) AS cnt FROM t_outbox_event GROUP BY status ORDER BY status"));
        result.put("exceptionsByStatus",
                jdbcTemplate.queryForList("SELECT status, COUNT(*) AS cnt FROM t_sync_exception GROUP BY status ORDER BY status"));
        return result;
    }

    public void reset() {
        jdbcTemplate.update("DELETE FROM t_outbox_event");
        jdbcTemplate.update("DELETE FROM t_sync_exception");
        jdbcTemplate.update("DELETE FROM t_order");
    }
}
