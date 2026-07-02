package com.aftersales.platform.common.domain;

import com.aftersales.platform.common.domain.Enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * 跨服务传输的订单快照，供 Agent 判断状态、延迟发货和取消条件。
 */
public record OrderInfo(Long id, Long userId, Long productId, OrderStatus status, LocalDateTime createdAt,
                        LocalDateTime paidAt, LocalDateTime shippedAt, String cancelReason) {
}
