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

    /** 分类后执行 Planner 生成并通过白名单校验的计划。 */
    public AgentResponse execute(String input, Long replayFromRunId) {
        Long runId = trace.createRun(input, TaskType.UNKNOWN, replayFromRunId);
        try {
            TaskType type = roles.classify(runId, input);
            if (type == TaskType.UNKNOWN && trace.hasModelFailure(runId, "supervisor.classify")) {
                throw new AiUnavailableException("智能任务识别暂时不可用，且当前输入无法通过确定性规则安全分类，请稍后重试。");
            }
            trace.updateTaskType(runId, type);
            trace.step(runId, "Supervisor", "任务分类", type.name());

            ExecutionPlan plan = validator.validateOrFallback(runId, type, roles.plan(runId, type, input));
            String actions = plan.steps().stream()
                    .map(step -> step.action().name())
                    .collect(Collectors.joining(" -> "));
            trace.step(runId, "Planner", "可执行计划", actions);

            PlanExecutor.PlanExecutionResult result = executor.execute(runId, input, plan);
            trace.finish(runId, result.status(), result.answer());
            AgentRun run = trace.run(runId);
            return new AgentResponse(runId, type.name(), run.status().name(), result.answer(), trace.warnings(runId));
        } catch (RuntimeException exception) {
            trace.finish(runId, RunStatus.FAILED, exception.getMessage());
            throw new RunExecutionException(runId, exception);
        }
    }

    public record AgentResponse(Long runId, String taskType, String status, String answer,
                                List<TraceService.RunWarning> warnings) {}
}
