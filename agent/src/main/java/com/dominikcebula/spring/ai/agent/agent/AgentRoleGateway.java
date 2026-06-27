package com.dominikcebula.spring.ai.agent.agent;

import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import com.dominikcebula.spring.ai.agent.domain.Enums.TaskType;
import com.dominikcebula.spring.ai.agent.domain.Product;
import com.dominikcebula.spring.ai.agent.service.TraceService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Supplier;

/**
 * 分布式角色调度入口。启用后通过 Nacos 服务名调用 Agent Worker，
 * Worker 不可用时回退本地角色，保证核心业务仍可执行。
 */
@Component
public class AgentRoleGateway {
    private final RestClient restClient;
    private final HarnessProperties properties;
    private final TraceService trace;
    private final SupervisorAgent localSupervisor;
    private final PlannerAgent localPlanner;
    private final RiskAgent localRisk;
    private final ReporterAgent localReporter;

    public AgentRoleGateway(RestClient.Builder builder, HarnessProperties properties,
                            TraceService trace, SupervisorAgent localSupervisor,
                            PlannerAgent localPlanner, RiskAgent localRisk,
                            ReporterAgent localReporter) {
        this.restClient = builder.build();
        this.properties = properties;
        this.trace = trace;
        this.localSupervisor = localSupervisor;
        this.localPlanner = localPlanner;
        this.localRisk = localRisk;
        this.localReporter = localReporter;
    }

    public TaskType classify(Long runId, String input) {
        return remoteOrLocal(
                runId,
                "Supervisor",
                () -> post("/classify", new ClassifyRequest(runId, input), TaskType.class),
                () -> localSupervisor.classify(runId, input)
        );
    }

    public List<String> plan(Long runId, TaskType taskType, String input) {
        return remoteOrLocal(
                runId,
                "Planner",
                () -> restClient.post()
                        .uri(url("/plan"))
                        .body(new PlanRequest(runId, taskType, input))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<String>>() {}),
                () -> localPlanner.plan(runId, taskType, input)
        );
    }

    public boolean sensitive(Long runId, String action) {
        Boolean result = remoteOrLocal(
                runId,
                "Risk Agent",
                () -> post("/risk", new RiskRequest(runId, action), Boolean.class),
                () -> localRisk.sensitive(action)
        );
        return Boolean.TRUE.equals(result);
    }

    public String afterSalesReply(Long runId, Long orderId, String productName, String policy) {
        return remoteOrLocal(
                runId,
                "Reporter Agent",
                () -> post("/report/after-sales",
                        new AfterSalesReportRequest(runId, orderId, productName, policy),
                        String.class),
                () -> localReporter.afterSalesReply(runId, orderId, productName, policy)
        );
    }

    public String productAdvice(Long runId, List<Product> products) {
        return remoteOrLocal(
                runId,
                "Reporter Agent",
                () -> post("/report/products", new ProductReportRequest(runId, products), String.class),
                () -> localReporter.productAdvice(runId, products)
        );
    }

    public String cancelApproved(Long orderId) {
        return localReporter.cancelApproved(orderId);
    }

    public String cancelWaiting(Long approvalId, Long orderId) {
        return localReporter.cancelWaiting(approvalId, orderId);
    }

    private <T> T post(String path, Object body, Class<T> resultType) {
        return restClient.post()
                .uri(url(path))
                .body(body)
                .retrieve()
                .body(resultType);
    }

    private <T> T remoteOrLocal(Long runId, String role,
                                Supplier<T> remoteCall, Supplier<T> localCall) {
        if (!properties.isDistributedAgentsEnabled()) {
            return localCall.get();
        }
        try {
            T result = remoteCall.get();
            if (result == null) {
                throw new IllegalStateException("远程 Agent 返回空结果");
            }
            trace.step(runId, "Agent Dispatcher", role, "REMOTE");
            return result;
        } catch (RuntimeException exception) {
            trace.step(runId, "Agent Dispatcher", role,
                    "REMOTE_FAILED, fallback=LOCAL, error=" + exception.getMessage());
            return localCall.get();
        }
    }

    private String url(String path) {
        return properties.getAgentWorkerUrl() + "/internal/agents" + path;
    }

    private record ClassifyRequest(Long runId, String input) {}
    private record PlanRequest(Long runId, TaskType taskType, String input) {}
    private record RiskRequest(Long runId, String action) {}
    private record AfterSalesReportRequest(
            Long runId, Long orderId, String productName, String policy) {}
    private record ProductReportRequest(Long runId, List<Product> products) {}
}
