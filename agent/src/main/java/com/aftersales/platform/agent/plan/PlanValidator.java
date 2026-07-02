package com.aftersales.platform.agent.plan;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.service.TraceService;
import com.aftersales.platform.common.domain.Enums.TaskType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 计划安全校验器，检查任务类型、步骤数量、动作顺序、条件和审批约束。
 */
@Component
public class PlanValidator {
    private final HarnessProperties properties;
    private final TraceService trace;

    public PlanValidator(HarnessProperties properties, TraceService trace) {
        this.properties = properties;
        this.trace = trace;
    }

    /**
     * 只接受白名单动作和安全顺序；任何异常计划都回退为受控模板。
     */
    public ExecutionPlan validateOrFallback(Long runId, TaskType type, ExecutionPlan candidate) {
        // ===== 1) 对模型计划执行纯校验，不在校验阶段触碰任何业务数据 =====
        String reason = validate(type, candidate);
        if (reason == null) {
            return candidate;
        }
        // ===== 2) 非法计划不直接执行：记录原因并回退到代码内维护的安全模板 =====
        trace.step(runId, "Plan Validator", "执行计划校验", "INVALID_PLAN, fallback=STANDARD, reason=" + reason);
        return PlanTemplates.forType(type);
    }

    private String validate(TaskType type, ExecutionPlan plan) {
        if (plan == null || plan.steps() == null) {
            return "计划为空";
        }
        if (plan.taskType() != type) {
            return "任务类型不一致";
        }
        // ===== 步骤数先受上限约束，防止模型输出超长计划造成循环调用 =====
        int maxSteps = properties.getMaxToolCalls() <= 0 ? 20 : properties.getMaxToolCalls() + 2;
        if (plan.steps().isEmpty() || plan.steps().size() > maxSteps) {
            return "步骤数量非法";
        }
        // ===== 检查步骤必填字段和 ID 唯一性，保证 Trace 能准确定位步骤 =====
        Set<String> ids = new HashSet<>();
        for (PlanStep step : plan.steps()) {
            if (step == null || step.id() == null || step.id().isBlank() || step.action() == null) {
                return "步骤字段缺失";
            }
            if (!ids.add(step.id())) {
                return "步骤ID重复";
            }
        }
        // ===== 与标准模板逐项比较，尤其不能删除取消审批或改变条件顺序 =====
        List<PlanStep> expected = PlanTemplates.forType(type).steps();
        if (plan.steps().size() != expected.size()) {
            return "缺少必要步骤";
        }
        for (int i = 0; i < expected.size(); i++) {
            PlanStep actual = plan.steps().get(i);
            PlanStep required = expected.get(i);
            if (actual.action() != required.action()) {
                return "动作顺序不安全";
            }
            if (!Objects.equals(actual.condition(), required.condition())) {
                return "条件约束不正确";
            }
            if (actual.requiresApproval() != required.requiresApproval()) {
                return "审批约束不正确";
            }
        }
        return null;
    }
}
