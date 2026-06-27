package com.dominikcebula.spring.ai.agent.repository;

import com.dominikcebula.spring.ai.agent.domain.ModelCallLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ModelCallTraceRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<ModelCallLog> mapper = (rs, rowNum) -> new ModelCallLog(
            rs.getLong("id"),
            rs.getLong("run_id"),
            rs.getString("scene"),
            rs.getString("model_name"),
            rs.getLong("elapsed_ms"),
            rs.getString("status"),
            rs.getString("error_message"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public ModelCallTraceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Long runId, String scene, String modelName, long elapsedMs,
                     String status, String errorMessage) {
        jdbc.update(
                "insert into model_call_log(run_id,scene,model_name,elapsed_ms,status,error_message,created_at) "
                        + "values(?,?,?,?,?,?,now())",
                runId, scene, modelName, elapsedMs, status, errorMessage
        );
    }

    public List<ModelCallLog> findByRunId(Long runId) {
        return jdbc.query(
                "select * from model_call_log where run_id=? order by id",
                mapper,
                runId
        );
    }
}
