package com.aftersales.platform.agent.domain;

import java.time.LocalDateTime;

/**
 * Agent 执行步骤读模型，用于前端按时间顺序展示完整运行轨迹。
 */
public record AgentStep(Long id, Long runId, String agentName, String stepName, String result,
                        LocalDateTime createdAt) {
}
