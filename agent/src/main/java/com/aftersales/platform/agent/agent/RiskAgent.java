package com.aftersales.platform.agent.agent;

import org.springframework.stereotype.Component;

/**
 * Risk 角色，识别敏感工具；当前取消订单必须进入人工审批流程。
 */
@Component
public class RiskAgent {
    /**
     * 取消订单会改变交易状态，固定视为敏感操作，必须走审批。
     */
    public boolean sensitive(String actionName) {
        return "cancelOrder".equals(actionName);
    }
}
