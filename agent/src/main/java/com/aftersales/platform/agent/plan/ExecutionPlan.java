package com.aftersales.platform.agent.plan;

import com.aftersales.platform.common.domain.Enums.TaskType;

import java.util.List;

/**
 * Planner 输出的结构化执行计划，由任务类型和有序步骤列表组成。
 */
public record ExecutionPlan(TaskType taskType, List<PlanStep> steps) {
}
