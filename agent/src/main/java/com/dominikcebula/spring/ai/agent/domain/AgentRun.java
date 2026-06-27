package com.dominikcebula.spring.ai.agent.domain;
import com.dominikcebula.spring.ai.agent.domain.Enums.RunStatus;
import com.dominikcebula.spring.ai.agent.domain.Enums.TaskType;
import java.time.LocalDateTime;
public record AgentRun(Long id, String userInput, TaskType taskType, RunStatus status, String finalAnswer, Long replayFromRunId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
