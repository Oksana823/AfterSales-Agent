package com.dominikcebula.spring.ai.agent.domain;
import com.dominikcebula.spring.ai.agent.domain.Enums.OrderStatus;
import java.time.LocalDateTime;
public record OrderInfo(Long id, Long userId, Long productId, OrderStatus status, LocalDateTime createdAt, LocalDateTime paidAt, LocalDateTime shippedAt, String cancelReason) {}
