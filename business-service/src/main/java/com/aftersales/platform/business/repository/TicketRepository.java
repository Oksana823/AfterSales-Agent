package com.aftersales.platform.business.repository;

import com.aftersales.platform.common.domain.Enums.TicketStatus;
import com.aftersales.platform.common.domain.Ticket;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
public class TicketRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Ticket> mapper = (rs, n) -> new Ticket(rs.getLong("id"), rs.getLong("order_id"), rs.getLong("user_id"), rs.getLong("product_id"), rs.getString("reason"), TicketStatus.valueOf(rs.getString("status")), rs.getString("customer_reply"), rs.getTimestamp("created_at").toLocalDateTime());
    public TicketRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Ticket create(Long orderId, Long userId, Long productId, String reason, String reply) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> { PreparedStatement ps = c.prepareStatement("insert into ticket(order_id,user_id,product_id,reason,status,customer_reply,created_at) values(?,?,?,?,?,?,now())", Statement.RETURN_GENERATED_KEYS); ps.setLong(1, orderId); ps.setLong(2, userId); ps.setLong(3, productId); ps.setString(4, reason); ps.setString(5, TicketStatus.OPEN.name()); ps.setString(6, reply); return ps; }, keys);
        return findById(keys.getKey().longValue()).orElseThrow();
    }
    public int updateCustomerReply(Long id, String reply) { return jdbc.update("update ticket set customer_reply=? where id=?", reply, id); }
    public Optional<Ticket> findById(Long id) { return jdbc.query("select * from ticket where id=?", mapper, id).stream().findFirst(); }
    public List<Ticket> findByOrderId(Long orderId) { return jdbc.query("select * from ticket where order_id=? order by created_at desc", mapper, orderId); }
}
