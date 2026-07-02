package com.aftersales.platform.agent.domain;

import com.aftersales.platform.common.domain.Enums.RunStatus;
import com.aftersales.platform.common.domain.Enums.TaskType;

import java.time.LocalDateTime;

/**
 * 一次自然语言任务的运行模型，保存任务类型、状态、结果及 Replay 来源。
 */
public record AgentRun(Long id, String userInput, TaskType taskType, RunStatus status, String finalAnswer,
                       Long replayFromRunId, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
