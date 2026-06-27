package com.aftersales.platform.agent.repository;

import com.aftersales.platform.agent.domain.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Product> mapper = (rs, n) -> new Product(rs.getLong("id"), rs.getString("name"), rs.getString("category"), rs.getString("brand"), rs.getBigDecimal("price"), rs.getString("tags"), rs.getString("description"), rs.getString("after_sales_policy"));
    public ProductRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<Product> findById(Long id) { return jdbc.query("select * from product where id=?", mapper, id).stream().findFirst(); }
    public List<Product> findAll() { return jdbc.query("select * from product", mapper); }
}
