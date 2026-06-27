package com.dominikcebula.spring.ai.agent.agent;

import com.dominikcebula.spring.ai.agent.domain.Enums.TaskType;
import com.dominikcebula.spring.ai.agent.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
        PlannerAgent planner = new PlannerAgent(unavailableModel);

        assertThat(planner.plan(1L, TaskType.CANCEL_ORDER, "取消订单10001"))
                .contains("识别敏感操作", "创建审批单", "等待审批后取消订单");
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
                .contains("AirLite 14 学生轻薄本", "4999.00", "适合上课和通勤");
    }

    private static AiModelService unavailableModel() {
        AiModelService model = mock(AiModelService.class);
        when(model.generate(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        return model;
    }
}
