package com.aftersales.platform.agent.mcp;

import com.aftersales.platform.agent.domain.OrderInfo;
import com.aftersales.platform.agent.service.OrderService;
import com.aftersales.platform.agent.service.ToolLogService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderTools {
    private final OrderService orderService;
    private final ToolLogService log;
    public OrderTools(OrderService orderService, ToolLogService log) { this.orderService = orderService; this.log = log; }

    @Tool(description = "查询用户最近一笔订单")
    public OrderInfo getLatestOrder(@ToolParam(description = "Agent run id") Long runId, @ToolParam(description = "用户ID") Long userId) {
        return log.call(runId, "order.getLatestOrder", Map.of("userId", userId), () -> orderService.latestOrder(userId));
    }

    @Tool(description = "查询订单详情")
    public OrderInfo getOrder(@ToolParam(description = "Agent run id") Long runId, @ToolParam(description = "订单ID") Long orderId) {
        return log.call(runId, "order.getOrder", Map.of("orderId", orderId), () -> orderService.getOrder(orderId));
    }

    @Tool(description = "判断订单是否已付款但超过阈值未发货")
    public Boolean isDelayedShipment(@ToolParam(description = "Agent run id") Long runId, @ToolParam(description = "订单ID") Long orderId) {
        return log.call(runId, "order.isDelayedShipment", Map.of("orderId", orderId), () -> orderService.isDelayedShipment(orderId));
    }

    @Tool(description = "取消订单。敏感操作，Agent 必须在审批通过后调用")
    public OrderInfo cancelOrder(@ToolParam(description = "Agent run id") Long runId, @ToolParam(description = "订单ID") Long orderId, @ToolParam(description = "取消原因") String reason) {
        return log.call(runId, "order.cancelOrder", Map.of("orderId", orderId, "reason", reason), () -> orderService.cancelOrder(runId, orderId, reason));
    }
}
