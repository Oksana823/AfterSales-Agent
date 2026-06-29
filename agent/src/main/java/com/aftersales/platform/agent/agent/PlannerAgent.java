package com.aftersales.platform.agent.agent;

import com.aftersales.platform.agent.plan.ExecutionPlan;
import com.aftersales.platform.agent.plan.PlanTemplates;
import com.aftersales.platform.common.domain.Enums.TaskType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent {
    private static final String SYSTEM_PROMPT = """
            你是售后平台的受约束 Planner。只输出一个JSON对象，不要解释。
            格式：{"taskType":"枚举","steps":[{"id":"唯一ID","action":"动作枚举","condition":null或指定条件,"requiresApproval":false}]}
            AFTER_SALES动作必须依次为：GET_LATEST_ORDER、CHECK_DELAYED_SHIPMENT、GET_PRODUCT、GET_AFTER_SALES_POLICY、CREATE_TICKET、GENERATE_AFTER_SALES_REPLY、UPDATE_TICKET_REPLY；后五步condition必须为delayed == true。
            CANCEL_ORDER动作必须依次为：GET_ORDER、VALIDATE_CANCELLABLE、CREATE_CANCEL_APPROVAL、CANCEL_ORDER；最后一步condition为approval == APPROVED且requiresApproval为true。
            PRODUCT_CONSULTATION动作必须依次为：SEARCH_PRODUCTS、GENERATE_PRODUCT_ADVICE。
            UNKNOWN只有RETURN_UNSUPPORTED。不得输出其他动作。
            """;
    private final AiModelService aiModel;
    private final ObjectMapper mapper;
    public PlannerAgent(AiModelService aiModel, ObjectMapper mapper) { this.aiModel = aiModel; this.mapper = mapper; }

    public ExecutionPlan plan(Long runId, TaskType type, String input) {
        String request = "任务类型：" + type.name() + System.lineSeparator() + "用户输入：" + input;
        AiModelService.ModelResult result = aiModel.generate(runId, "planner.plan", SYSTEM_PROMPT, request);
        if (!result.success()) return PlanTemplates.forType(type);
        try {
            String content = result.content();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalArgumentException("缺少JSON对象");
            return mapper.readValue(content.substring(start, end + 1), ExecutionPlan.class);
        } catch (Exception exception) {
            aiModel.recordInvalidOutput(runId, "planner.plan");
            return PlanTemplates.forType(type);
        }
    }
}
