package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.domain.*;
import com.aftersales.platform.agent.repository.TraceRepository;
import com.aftersales.platform.common.domain.Enums.RunStatus;
import com.aftersales.platform.common.domain.Enums.TaskType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Harness Trace 门面，统一维护 Run 生命周期、Agent 步骤、降级告警和 Redis 运行状态。
 */
@Service
public class TraceService {
    private final TraceRepository repository;
    private final ModelCallTraceService modelCalls;
    private final StringRedisTemplate redis;

    public TraceService(TraceRepository repository, ModelCallTraceService modelCalls, StringRedisTemplate redis) {
        this.repository = repository;
        this.modelCalls = modelCalls;
        this.redis = redis;
    }

    public Long createRun(String input, TaskType type, Long replayFromRunId) {
        // ===== 创建数据库 Run 后同步缓存 RUNNING 状态，供快速状态查询和运行保护使用 =====
        Long id = repository.createRun(input, type, replayFromRunId);
        cacheStatus(id, RunStatus.RUNNING);
        return id;
    }

    public void updateTaskType(Long runId, TaskType type) {
        repository.updateTaskType(runId, type);
    }

    public void step(Long runId, String agent, String step, String result) {
        repository.addStep(runId, agent, step, result);
    }

    public void finish(Long runId, RunStatus requested, String answer) {
        // ===== 只有请求状态为 COMPLETED 时才检查降级；失败和等待审批保持原状态 =====
        RunStatus actual =
                requested == RunStatus.COMPLETED && degraded(runId) ? RunStatus.COMPLETED_WITH_WARNINGS : requested;
        repository.updateRun(runId, actual, answer);
        cacheStatus(runId, actual);
    }

    public boolean hasModelFailure(Long runId, String scene) {
        return modelCalls.hasNonSuccess(runId, scene);
    }

    public List<RunWarning> warnings(Long runId) {
        List<RunWarning> warnings = new ArrayList<>();
        // ===== 汇总模型调用失败，生成前端可展示或仅审计的降级告警 =====
        for (ModelCallLog call : modelCalls.findByRunId(runId)) {
            if ("SUCCESS".equals(call.status())) {
                continue;
            }
            boolean reporter = call.scene().startsWith("reporter.");
            String message = reporter
                    ? ("reporter.productAdvice".equals(
                    call.scene()) ? "智能推荐暂时不可用，已返回未经过 AI 分析的检索结果。" :
                    "智能客服暂时不可用，已返回系统确认的处理结果。")
                    : "模型能力已降级，当前步骤使用确定性规则完成。";
            warnings.add(new RunWarning("LLM_" + call.status(), call.scene(), message, reporter));
        }
        // ===== 补充计划回退和远程 Agent 回退告警，使“看似成功”的降级不会被掩盖 =====
        if (repository.hasPlanFallback(runId)) {
            warnings.add(
                    new RunWarning("INVALID_PLAN", "planner.validation", "模型计划未通过安全校验，已使用标准执行计划。",
                            false));
        }
        if (repository.hasRemoteFailure(runId)) {
            warnings.add(
                    new RunWarning("REMOTE_AGENT_UNAVAILABLE", "agent.dispatch",
                            "远程 Agent 不可用，已使用本地 Agent 执行。",
                            false));
        }
        return warnings;
    }

    public Map<String, Object> detail(Long runId) {
        return Map.of("run", run(runId), "steps", repository.findSteps(runId), "toolCalls",
                repository.findToolCalls(runId), "modelCalls", modelCalls.findByRunId(runId), "warnings",
                warnings(runId));
    }

    public List<ToolCallLog> toolCalls(Long runId) {
        return repository.findToolCalls(runId);
    }

    public AgentRun run(Long runId) {
        return repository.findRun(runId).orElseThrow(() -> new IllegalArgumentException("run不存在"));
    }

    private boolean degraded(Long runId) {
        return modelCalls.hasNonSuccess(runId) || repository.hasPlanFallback(runId) ||
                repository.hasRemoteFailure(runId);
    }

    private void cacheStatus(Long runId, RunStatus status) {
        redis.opsForValue().set("agent:run:" + runId + ":status", status.name(), 30, TimeUnit.MINUTES);
    }

    public record RunWarning(String code, String scene, String message, boolean userVisible) {
    }
}
