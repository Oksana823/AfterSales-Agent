package com.aftersales.platform.agent.domain;

import java.time.LocalDateTime;

/**
 * LLM 调用日志读模型，对应 model_call_log 表中的可观测字段。
 */
public record ModelCallLog(
        Long id,
        Long runId,
        String scene,
        String modelName,
        long elapsedMs,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {
}
