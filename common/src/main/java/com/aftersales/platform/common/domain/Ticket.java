package com.aftersales.platform.common.domain;
import com.aftersales.platform.common.domain.Enums.TicketStatus;
import java.time.LocalDateTime;
public record Ticket(Long id, Long orderId, Long userId, Long productId, String reason, TicketStatus status, String customerReply, LocalDateTime createdAt) {}
