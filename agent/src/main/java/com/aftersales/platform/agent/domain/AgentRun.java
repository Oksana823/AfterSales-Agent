package com.aftersales.platform.agent.domain;
import com.aftersales.platform.common.domain.Enums.RunStatus;
import com.aftersales.platform.common.domain.Enums.TaskType;
import java.time.LocalDateTime;
public record AgentRun(Long id, String userInput, TaskType taskType, RunStatus status, String finalAnswer, Long replayFromRunId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
