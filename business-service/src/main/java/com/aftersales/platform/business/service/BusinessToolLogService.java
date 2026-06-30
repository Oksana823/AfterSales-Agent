package com.aftersales.platform.business.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class BusinessToolLogService {
    private final JdbcTemplate jdbc;
    private final JsonService json;

    public BusinessToolLogService(JdbcTemplate jdbc, JsonService json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public <T> T call(Long runId, String toolName, Object args, Supplier<T> action) {
        long started = System.currentTimeMillis();
        try {
            T result = action.get();
            save(runId, toolName, args, result, System.currentTimeMillis() - started, "SUCCESS", null);
            return result;
        } catch (RuntimeException e) {
            save(runId, toolName, args, null, System.currentTimeMillis() - started, "FAILED", e.getMessage());
            throw e;
        }
    }

    private void save(Long runId, String name, Object args, Object result, long elapsed, String status, String error) {
        jdbc.update(
                "insert into tool_call_log(run_id,tool_name,arguments_json,result_json,elapsed_ms,status," +
                        "error_message,created_at) values(?,?,?,?,?,?,?,now())",
                runId, name, json.toJson(args), result == null ? null : json.toJson(result), elapsed, status, error);
    }
}
