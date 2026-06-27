package com.aftersales.platform.agent.domain;
import com.aftersales.platform.agent.domain.Enums.OrderStatus;
import java.time.LocalDateTime;
public record OrderInfo(Long id, Long userId, Long productId, OrderStatus status, LocalDateTime createdAt, LocalDateTime paidAt, LocalDateTime shippedAt, String cancelReason) {}
