package com.aftersales.platform.common.domain;

import java.math.BigDecimal;

/**
 * 跨服务传输的商品数据，既用于 MySQL 查询，也作为 Elasticsearch 商品文档模型。
 */
public record Product(Long id, String name, String category, String brand, BigDecimal price, String tags,
                      String description, String afterSalesPolicy) {
}
