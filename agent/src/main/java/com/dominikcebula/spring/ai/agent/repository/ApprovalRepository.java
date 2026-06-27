package com.dominikcebula.spring.ai.agent.repository;

import com.dominikcebula.spring.ai.agent.domain.ApprovalRequest;
import com.dominikcebula.spring.ai.agent.domain.Enums.ApprovalStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ApprovalRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<ApprovalRequest> mapper = (rs, n) -> new ApprovalRequest(
            rs.getLong("id"),
            rs.getLong("run_id"),
            rs.getString("action_name"),
            rs.getLong("order_id"),
            rs.getString("reason"),
            ApprovalStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("handled_at") == null ? null : rs.getTimestamp("handled_at").toLocalDateTime()
    );

    public ApprovalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ApprovalRequest create(Long runId, String actionName, Long orderId, String reason) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into approval_request(run_id,action_name,order_id,reason,status,created_at) "
                            + "values(?,?,?,?,?,now())",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, runId);
            statement.setString(2, actionName);
            statement.setLong(3, orderId);
            statement.setString(4, reason);
            statement.setString(5, ApprovalStatus.PENDING.name());
            return statement;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public Optional<ApprovalRequest> findById(Long id) {
        return jdbc.query("select * from approval_request where id=?", mapper, id)
                .stream()
                .findFirst();
    }

    public List<ApprovalRequest> pending() {
        return jdbc.query(
                "select * from approval_request where status='PENDING' order by created_at",
                mapper
        );
    }

    public int updateStatus(Long id, ApprovalStatus status) {
        return jdbc.update(
                "update approval_request set status=?, handled_at=now() "
                        + "where id=? and status='PENDING'",
                status.name(),
                id
        );
    }
}
