package com.aftersales.platform.agent.domain;

import java.time.LocalDateTime;

public record AgentStep(Long id, Long runId, String agentName, String stepName, String result,
                        LocalDateTime createdAt) {
}
