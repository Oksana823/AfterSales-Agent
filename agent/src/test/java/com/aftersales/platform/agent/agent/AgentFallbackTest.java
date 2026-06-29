package com.aftersales.platform.agent.agent;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.plan.ExecutionPlan;
import com.aftersales.platform.agent.plan.PlanAction;
import com.aftersales.platform.agent.plan.PlanStep;
import com.aftersales.platform.agent.plan.PlanValidator;
import com.aftersales.platform.agent.service.TraceService;
import com.aftersales.platform.common.domain.Enums.TaskType;
import com.aftersales.platform.common.domain.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentFallbackTest {

    private final AiModelService unavailableModel = unavailableModel();

    @Test
    void supervisorFallsBackToRulesWhenModelIsUnavailable() {
        SupervisorAgent supervisor = new SupervisorAgent(unavailableModel);

        assertThat(supervisor.classify(1L, "帮我取消订单10001"))
                .isEqualTo(TaskType.CANCEL_ORDER);
        assertThat(supervisor.classify(1L, "推荐5000元轻薄本"))
                .isEqualTo(TaskType.PRODUCT_CONSULTATION);
    }

    @Test
    void cancellationPlanAlwaysKeepsApprovalStep() {
        PlannerAgent planner = new PlannerAgent(unavailableModel, new ObjectMapper());

        assertThat(planner.plan(1L, TaskType.CANCEL_ORDER, "取消订单10001").steps())
                .extracting(step -> step.action())
                .containsExactly(
                        PlanAction.GET_ORDER,
                        PlanAction.VALIDATE_CANCELLABLE,
                        PlanAction.CREATE_CANCEL_APPROVAL,
                        PlanAction.CANCEL_ORDER
                );
    }

    @Test
    void reporterUsesSearchResultsWithoutInventingProducts() {
        ReporterAgent reporter = new ReporterAgent(unavailableModel);
        Product product = new Product(
                1L,
                "AirLite 14 学生轻薄本",
                "Laptop",
                "Aster",
                new BigDecimal("4999.00"),
                "学生,轻薄本",
                "适合上课和通勤",
                "延迟发货可创建工单"
        );

        assertThat(reporter.productAdvice(1L, List.of(product)))
                .contains("智能推荐暂时不可用", "AirLite 14 学生轻薄本", "4999.00", "适合上课和通勤");
    }

    @Test
    void unsafePlanFallsBackToApprovedTemplate() {
        TraceService trace = mock(TraceService.class);
        PlanValidator validator = new PlanValidator(new HarnessProperties(), trace);
        ExecutionPlan unsafe = new ExecutionPlan(
                TaskType.CANCEL_ORDER,
                List.of(new PlanStep("cancel", PlanAction.CANCEL_ORDER, null, false))
        );

        assertThat(validator.validateOrFallback(1L, TaskType.CANCEL_ORDER, unsafe).steps())
                .extracting(PlanStep::action)
                .containsExactly(
                        PlanAction.GET_ORDER,
                        PlanAction.VALIDATE_CANCELLABLE,
                        PlanAction.CREATE_CANCEL_APPROVAL,
                        PlanAction.CANCEL_ORDER
                );
    }
    private static AiModelService unavailableModel() {
        AiModelService model = mock(AiModelService.class);
        when(model.generate(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString())).thenReturn(new AiModelService.ModelResult(AiModelService.ModelStatus.UNAVAILABLE, null));
        return model;
    }
}
