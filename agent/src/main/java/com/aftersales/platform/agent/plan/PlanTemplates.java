package com.aftersales.platform.agent.plan;

import com.aftersales.platform.common.domain.Enums.TaskType;

import java.util.List;

/**
 * 确定性标准计划模板，在模型不可用或计划非法时提供安全回退流程。
 */
public final class PlanTemplates {
    private PlanTemplates() {
    }

    public static ExecutionPlan forType(TaskType type) {
        return new ExecutionPlan(type, switch (type) {
            case AFTER_SALES -> List.of(
                    step("latestOrder", PlanAction.GET_LATEST_ORDER),
                    step("delayed", PlanAction.CHECK_DELAYED_SHIPMENT),
                    conditional("product", PlanAction.GET_PRODUCT),
                    conditional("policy", PlanAction.GET_AFTER_SALES_POLICY),
                    conditional("ticket", PlanAction.CREATE_TICKET),
                    conditional("reply", PlanAction.GENERATE_AFTER_SALES_REPLY),
                    conditional("updateReply", PlanAction.UPDATE_TICKET_REPLY));
            case CANCEL_ORDER -> List.of(
                    step("order", PlanAction.GET_ORDER),
                    step("validate", PlanAction.VALIDATE_CANCELLABLE),
                    step("approval", PlanAction.CREATE_CANCEL_APPROVAL),
                    new PlanStep("cancel", PlanAction.CANCEL_ORDER, "approval == APPROVED", true));
            case PRODUCT_CONSULTATION -> List.of(
                    step("products", PlanAction.SEARCH_PRODUCTS),
                    step("advice", PlanAction.GENERATE_PRODUCT_ADVICE));
            case UNKNOWN -> List.of(step("unsupported", PlanAction.RETURN_UNSUPPORTED));
        });
    }

    private static PlanStep step(String id, PlanAction action) {
        return new PlanStep(id, action, null, false);
    }

    private static PlanStep conditional(String id, PlanAction action) {
        return new PlanStep(id, action, "delayed == true", false);
    }
}
