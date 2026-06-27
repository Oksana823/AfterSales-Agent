package com.aftersales.platform.business.repository;

import com.aftersales.platform.common.domain.Enums.OrderStatus;
import com.aftersales.platform.common.domain.OrderInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<OrderInfo> mapper = (rs, n) -> new OrderInfo(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("product_id"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime(),
            rs.getTimestamp("shipped_at") == null ? null : rs.getTimestamp("shipped_at").toLocalDateTime(),
            rs.getString("cancel_reason")
    );

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OrderInfo> findById(Long id) {
        return jdbc.query("select * from order_info where id=?", mapper, id).stream().findFirst();
    }

    public Optional<OrderInfo> findLatestByUserId(Long userId) {
        return jdbc.query(
                "select * from order_info where user_id=? order by created_at desc limit 1",
                mapper,
                userId
        ).stream().findFirst();
    }

    public int cancel(Long id, String reason) {
        return jdbc.update(
                "update order_info set status='CANCELLED', cancel_reason=? "
                        + "where id=? and status in ('CREATED','PAID')",
                reason,
                id
        );
    }
}
