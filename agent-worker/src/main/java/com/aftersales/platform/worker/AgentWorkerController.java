package com.aftersales.platform.worker;

import com.aftersales.platform.agent.agent.PlannerAgent;
import com.aftersales.platform.agent.agent.ReporterAgent;
import com.aftersales.platform.agent.agent.RiskAgent;
import com.aftersales.platform.agent.agent.SupervisorAgent;
import com.aftersales.platform.common.domain.Enums.TaskType;
import com.aftersales.platform.agent.plan.ExecutionPlan;
import com.aftersales.platform.common.domain.Product;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/agents")
public class AgentWorkerController {
    private final SupervisorAgent supervisor;
    private final PlannerAgent planner;
    private final RiskAgent risk;
    private final ReporterAgent reporter;

    public AgentWorkerController(SupervisorAgent supervisor, PlannerAgent planner,
                                 RiskAgent risk, ReporterAgent reporter) {
        this.supervisor = supervisor;
        this.planner = planner;
        this.risk = risk;
        this.reporter = reporter;
    }

    @PostMapping("/classify")
    public TaskType classify(@RequestBody ClassifyRequest request) {
        return supervisor.classify(request.runId(), request.input());
    }

    @PostMapping("/plan")
    public ExecutionPlan plan(@RequestBody PlanRequest request) {
        return planner.plan(request.runId(), request.taskType(), request.input());
    }

    @PostMapping("/risk")
    public boolean risk(@RequestBody RiskRequest request) {
        return risk.sensitive(request.action());
    }

    @PostMapping("/report/after-sales")
    public String afterSalesReport(@RequestBody AfterSalesReportRequest request) {
        return reporter.afterSalesReply(
                request.runId(), request.ticketId(), request.orderId(), request.productName(), request.policy()
        );
    }

    @PostMapping("/report/products")
    public String productReport(@RequestBody ProductReportRequest request) {
        return reporter.productAdvice(request.runId(), request.products());
    }

    public record ClassifyRequest(Long runId, String input) {}
    public record PlanRequest(Long runId, TaskType taskType, String input) {}
    public record RiskRequest(Long runId, String action) {}
    public record AfterSalesReportRequest(
            Long runId, Long ticketId, Long orderId, String productName, String policy) {}
    public record ProductReportRequest(Long runId, List<Product> products) {}
}
