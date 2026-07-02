package com.aftersales.platform.agent.domain;

import com.aftersales.platform.common.domain.Enums.ApprovalStatus;

import java.time.LocalDateTime;

/**
 * 取消订单审批单模型，记录关联 Run、订单、理由和处理状态。
 */
public record ApprovalRequest(Long id, Long runId, String actionName, Long orderId, String reason,
                              ApprovalStatus status, LocalDateTime createdAt, LocalDateTime handledAt) {
}
