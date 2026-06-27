package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.domain.AgentRun;
import com.aftersales.platform.agent.domain.Enums.RunStatus;
import com.aftersales.platform.agent.domain.Enums.TaskType;
import com.aftersales.platform.agent.domain.ToolCallLog;
import com.aftersales.platform.agent.repository.TraceRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TraceService {
    private final TraceRepository repository;
    private final ModelCallTraceService modelCalls;
    private final StringRedisTemplate redis;

    public TraceService(TraceRepository repository, ModelCallTraceService modelCalls,
                        StringRedisTemplate redis) {
        this.repository = repository;
        this.modelCalls = modelCalls;
        this.redis = redis;
    }

    public Long createRun(String input, TaskType type, Long replayFromRunId) {
        Long id = repository.createRun(input, type, replayFromRunId);
        redis.opsForValue().set(
                "agent:run:" + id + ":status",
                RunStatus.RUNNING.name(),
                30,
                TimeUnit.MINUTES
        );
        return id;
    }

    public void updateTaskType(Long runId, TaskType type) {
        repository.updateTaskType(runId, type);
    }

    public void step(Long runId, String agent, String step, String result) {
        repository.addStep(runId, agent, step, result);
    }

    public void tool(Long runId, String toolName, String args, String result,
                     long elapsedMs, String status, String error) {
        repository.addToolCall(runId, toolName, args, result, elapsedMs, status, error);
    }

    public void finish(Long runId, RunStatus status, String answer) {
        repository.updateRun(runId, status, answer);
        redis.opsForValue().set(
                "agent:run:" + runId + ":status",
                status.name(),
                30,
                TimeUnit.MINUTES
        );
    }

    public Map<String, Object> detail(Long runId) {
        AgentRun run = run(runId);
        return Map.of(
                "run", run,
                "steps", repository.findSteps(runId),
                "toolCalls", repository.findToolCalls(runId),
                "modelCalls", modelCalls.findByRunId(runId)
        );
    }

    public List<ToolCallLog> toolCalls(Long runId) {
        return repository.findToolCalls(runId);
    }

    public AgentRun run(Long runId) {
        return repository.findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("run不存在"));
    }
}
