package com.aftersales.platform.common.domain;

import com.aftersales.platform.common.domain.Enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * 跨服务传输的售后工单快照，字段与 MySQL ticket 表的核心业务字段对应。
 */
public record Ticket(Long id, Long orderId, Long userId, Long productId, String reason, TicketStatus status,
                     String customerReply, LocalDateTime createdAt) {
}
