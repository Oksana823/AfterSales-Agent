package com.aftersales.platform.common.domain;
import java.math.BigDecimal;
public record Product(Long id, String name, String category, String brand, BigDecimal price, String tags, String description, String afterSalesPolicy) {}
