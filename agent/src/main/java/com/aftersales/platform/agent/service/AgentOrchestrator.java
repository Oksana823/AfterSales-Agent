package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.agent.AgentRoleGateway;
import com.aftersales.platform.agent.domain.AgentRun;
import com.aftersales.platform.agent.plan.ExecutionPlan;
import com.aftersales.platform.agent.plan.PlanExecutor;
import com.aftersales.platform.agent.plan.PlanValidator;
import com.aftersales.platform.common.domain.Enums.RunStatus;
import com.aftersales.platform.common.domain.Enums.TaskType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 主编排入口，串联 Run 创建、任务分类、计划生成、安全校验、计划执行和结果收尾。
 */
@Service
public class AgentOrchestrator {
    private final AgentRoleGateway roles;
    private final TraceService trace;
    private final PlanValidator validator;
    private final PlanExecutor executor;

    public AgentOrchestrator(AgentRoleGateway roles, TraceService trace,
                             PlanValidator validator, PlanExecutor executor) {
        this.roles = roles;
        this.trace = trace;
        this.validator = validator;
        this.executor = executor;
    }

    public AgentResponse execute(String input) {
        return execute(input, null);
    }

    /**
     * 分类后执行 Planner 生成并通过白名单校验的计划。
     */
    public AgentResponse execute(String input, Long replayFromRunId) {
        // ===== 1) 先创建 Run，保证后续成功、失败和 Replay 都有统一审计主键 =====
        Long runId = trace.createRun(input, TaskType.UNKNOWN, replayFromRunId);
        try {
            // ===== 2) Supervisor 识别任务类型；模型不可用时仍可使用确定性规则分类 =====
            TaskType type = roles.classify(runId, input);
            if (type == TaskType.UNKNOWN && trace.hasModelFailure(runId, "supervisor.classify")) {
                throw new AiUnavailableException(
                        "智能任务识别暂时不可用，且当前输入无法通过确定性规则安全分类，请稍后重试。");
            }
            trace.updateTaskType(runId, type);
            trace.step(runId, "Supervisor", "任务分类", type.name());

            // ===== 3) Planner 生成结构化计划，Validator 拦截越权、缺步骤或错误顺序 =====
            ExecutionPlan plan = validator.validateOrFallback(runId, type, roles.plan(runId, type, input));
            String actions = plan.steps().stream()
                    .map(step -> step.action().name())
                    .collect(Collectors.joining(" -> "));
            trace.step(runId, "Planner", "可执行计划", actions);

            // ===== 4) PlanExecutor 真正按计划调用 Agent 角色和 MCP 工具 =====
            PlanExecutor.PlanExecutionResult result = executor.execute(runId, input, plan);
            trace.finish(runId, result.status(), result.answer());
            AgentRun run = trace.run(runId);
            return new AgentResponse(runId, type.name(), run.status().name(), result.answer(), trace.warnings(runId));
            // ===== 5) 无论在哪一步失败，都先结束 Run，再把 runId 带给前端查询失败 Trace =====
        } catch (RuntimeException exception) {
            trace.finish(runId, RunStatus.FAILED, exception.getMessage());
            throw new RunExecutionException(runId, exception);
        }
    }

    public record AgentResponse(Long runId, String taskType, String status, String answer,
                                List<TraceService.RunWarning> warnings) {
    }
}
