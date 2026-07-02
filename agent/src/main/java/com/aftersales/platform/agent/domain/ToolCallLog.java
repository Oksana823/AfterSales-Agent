package com.aftersales.platform.agent.domain;

import java.time.LocalDateTime;

/**
 * MCP 工具调用日志读模型，对应 tool_call_log 表中的审计字段。
 */
public record ToolCallLog(Long id, Long runId, String toolName, String argumentsJson, String resultJson, long elapsedMs,
                          String status, String errorMessage, LocalDateTime createdAt) {
}
