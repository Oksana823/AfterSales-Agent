package com.aftersales.platform.business.repository;

import com.aftersales.platform.common.domain.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * product 表数据访问层，为详情、缓存和 ES 索引重建提供数据。
 */
@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Product> mapper = (rs, n) -> new Product(rs.getLong("id"), rs.getString("name"),
            rs.getString("category"), rs.getString("brand"), rs.getBigDecimal("price"), rs.getString("tags"),
            rs.getString("description"), rs.getString("after_sales_policy"));

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> findById(Long id) {
        return jdbc.query("select * from product where id=?", mapper, id).stream().findFirst();
    }

    public List<Product> findAll() {
        return jdbc.query("select * from product", mapper);
    }
}
