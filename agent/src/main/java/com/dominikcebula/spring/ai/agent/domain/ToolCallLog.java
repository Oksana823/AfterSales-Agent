package com.dominikcebula.spring.ai.agent.domain;
import java.time.LocalDateTime;
public record ToolCallLog(Long id, Long runId, String toolName, String argumentsJson, String resultJson, long elapsedMs, String status, String errorMessage, LocalDateTime createdAt) {}
