package com.dominikcebula.spring.ai.agent.domain;
import com.dominikcebula.spring.ai.agent.domain.Enums.ApprovalStatus;
import java.time.LocalDateTime;
public record ApprovalRequest(Long id, Long runId, String actionName, Long orderId, String reason, ApprovalStatus status, LocalDateTime createdAt, LocalDateTime handledAt) {}
