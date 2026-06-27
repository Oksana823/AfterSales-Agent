package com.aftersales.platform.agent.agent;

import com.aftersales.platform.agent.domain.Enums.TaskType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class PlannerAgent {
    private static final String SYSTEM_PROMPT = """
            你是售后订单平台的 Planner。
            根据任务类型和用户输入生成 3 到 6 个简短执行步骤，每行一个步骤。
            不要编号，不要解释，不得添加删除数据或绕过审批的操作。
            取消订单必须包含风险识别、创建审批、审批通过后执行取消。
            """;

    private final AiModelService aiModel;

    public PlannerAgent(AiModelService aiModel) {
        this.aiModel = aiModel;
    }

    /** 模型计划只用于解释和 Trace，实际执行边界仍由编排代码控制。 */
    public List<String> plan(Long runId, TaskType type, String input) {
        String request = "任务类型：" + type.name() + "\n用户输入：" + input;
        Optional<List<String>> modelPlan = aiModel.generate(runId, "planner.plan", SYSTEM_PROMPT, request)
                .map(this::parseSteps)
                .filter(steps -> !steps.isEmpty());
        return modelPlan.orElseGet(() -> rulePlan(type));
    }

    private List<String> parseSteps(String content) {
        return Arrays.stream(content.split("\\R"))
                .map(line -> line.replaceFirst("^\\s*(?:[-*]|\\d+[.、)])\\s*", "").trim())
                .filter(line -> !line.isBlank())
                .limit(6)
                .toList();
    }

    private List<String> rulePlan(TaskType type) {
        return switch (type) {
            case AFTER_SALES -> List.of("查询用户最近订单", "判断是否延迟发货", "查询售后政策", "创建工单", "生成客服回复和报告");
            case CANCEL_ORDER -> List.of("查询订单", "识别敏感操作", "创建审批单", "等待审批后取消订单");
            case PRODUCT_CONSULTATION -> List.of("提取商品需求", "调用 Elasticsearch 搜索", "生成推荐理由");
            default -> List.of("无法识别任务类型");
        };
    }
}
