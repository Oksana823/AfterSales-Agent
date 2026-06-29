package com.aftersales.platform.agent.agent;

import com.aftersales.platform.common.domain.Enums.TaskType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class SupervisorAgent {
    private static final String SYSTEM_PROMPT = """
            你是售后订单平台的 Supervisor。
            只允许输出以下一个枚举值，不要解释：
            AFTER_SALES、CANCEL_ORDER、PRODUCT_CONSULTATION、UNKNOWN。
            取消具体订单属于 CANCEL_ORDER；延迟发货、售后工单属于 AFTER_SALES；
            商品推荐和预算咨询属于 PRODUCT_CONSULTATION。
            """;

    private final AiModelService aiModel;

    public SupervisorAgent(AiModelService aiModel) {
        this.aiModel = aiModel;
    }

    /** 优先让模型识别任务；输出不合规或模型不可用时使用稳定规则回退。 */
    public TaskType classify(Long runId, String input) {
        AiModelService.ModelResult result = aiModel.generate(runId, "supervisor.classify", SYSTEM_PROMPT, input);
        if (result.success()) {
            Optional<TaskType> decision = parseTaskType(result.content());
            if (decision.isPresent()) return decision.get();
            aiModel.recordInvalidOutput(runId, "supervisor.classify");
        }
        return classifyByRules(input);
    }

    private Optional<TaskType> parseTaskType(String content) {
        String normalized = content.toUpperCase(Locale.ROOT);
        for (TaskType type : TaskType.values()) {
            if (normalized.contains(type.name())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private TaskType classifyByRules(String input) {
        if (input.contains("取消订单") || input.contains("取消 订单")) {
            return TaskType.CANCEL_ORDER;
        }
        if (input.contains("轻薄本") || input.contains("预算")
                || input.contains("推荐") || input.contains("找一个")) {
            return TaskType.PRODUCT_CONSULTATION;
        }
        if (input.contains("售后") || input.contains("未发货") || input.contains("工单")) {
            return TaskType.AFTER_SALES;
        }
        return TaskType.UNKNOWN;
    }
}
