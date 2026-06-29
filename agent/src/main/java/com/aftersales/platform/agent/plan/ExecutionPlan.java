package com.aftersales.platform.agent.plan;

import com.aftersales.platform.common.domain.Enums.TaskType;
import java.util.List;

public record ExecutionPlan(TaskType taskType, List<PlanStep> steps) {}
