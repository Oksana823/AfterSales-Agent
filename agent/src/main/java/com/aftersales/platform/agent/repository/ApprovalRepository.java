package com.aftersales.platform.agent.repository;

import com.aftersales.platform.agent.domain.ApprovalRequest;
import com.aftersales.platform.common.domain.Enums.ApprovalStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * approval_request 数据访问层，负责审批单创建、查询和条件状态更新。
 */
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
        // 插入后取回数据库自增主键，再查询完整审批对象返回给编排层。
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

    /**
     * 仅允许从 PENDING 更新，返回值为 0 表示审批已被其他请求处理。
     */
    public int updateStatus(Long id, ApprovalStatus status) {
        return jdbc.update(
                "update approval_request set status=?, handled_at=now() "
                        + "where id=? and status='PENDING'",
                status.name(),
                id
        );
    }
}
