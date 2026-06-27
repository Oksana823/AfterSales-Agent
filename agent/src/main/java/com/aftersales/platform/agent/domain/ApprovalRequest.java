package com.aftersales.platform.agent.domain;
import com.aftersales.platform.common.domain.Enums.ApprovalStatus;
import java.time.LocalDateTime;
public record ApprovalRequest(Long id, Long runId, String actionName, Long orderId, String reason, ApprovalStatus status, LocalDateTime createdAt, LocalDateTime handledAt) {}
