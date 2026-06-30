package com.aftersales.platform.agent.controller;

import com.aftersales.platform.agent.service.AgentOrchestrator;
import com.aftersales.platform.agent.service.ApprovalService;
import com.aftersales.platform.agent.service.TraceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentOrchestrator orchestrator;
    private final TraceService trace;
    private final ApprovalService approvals;

    public AgentController(AgentOrchestrator orchestrator, TraceService trace, ApprovalService approvals) {
        this.orchestrator = orchestrator;
        this.trace = trace;
        this.approvals = approvals;
    }

    @PostMapping("/chat")
    public AgentOrchestrator.AgentResponse chat(@Valid @RequestBody ChatRequest request) {
        return orchestrator.execute(request.message());
    }

    @GetMapping("/runs/{runId}")
    public Object run(@PathVariable Long runId) {
        return trace.detail(runId);
    }

    @GetMapping("/runs/{runId}/tool-calls")
    public Object toolCalls(@PathVariable Long runId) {
        return trace.toolCalls(runId);
    }

    @GetMapping("/runs/{runId}/model-calls")
    public Object modelCalls(@PathVariable Long runId) {
        return trace.detail(runId).get("modelCalls");
    }

    @PostMapping("/runs/{runId}/replay")
    public Object replay(@PathVariable Long runId) {
        return orchestrator.execute(trace.run(runId).userInput(), runId);
    }

    @GetMapping("/approvals/pending")
    public Object pendingApprovals() {
        return approvals.pending();
    }

    @PostMapping("/approvals/{id}/approve")
    public Object approve(@PathVariable Long id) {
        return Map.of("message", approvals.approve(id));
    }

    @PostMapping("/approvals/{id}/reject")
    public Object reject(@PathVariable Long id) {
        return Map.of("message", approvals.reject(id));
    }

    public record ChatRequest(@NotBlank(message = "message不能为空") String message) {
    }
}
