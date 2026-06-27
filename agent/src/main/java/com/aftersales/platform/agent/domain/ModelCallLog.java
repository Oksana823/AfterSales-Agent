package com.aftersales.platform.agent.domain;

import java.time.LocalDateTime;

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
