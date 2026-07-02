package com.aftersales.platform.agent.plan;

/**
 * 结构化计划中的单个步骤，描述动作、执行条件及是否需要人工审批。
 */
public record PlanStep(String id, PlanAction action, String condition, boolean requiresApproval) {
}
