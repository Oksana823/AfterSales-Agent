package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.domain.OrderInfo;
import com.aftersales.platform.agent.domain.Product;
import com.aftersales.platform.agent.domain.Ticket;
import com.aftersales.platform.agent.mcp.OrderTools;
import com.aftersales.platform.agent.mcp.ProductTools;
import com.aftersales.platform.agent.mcp.TicketTools;
import com.aftersales.platform.agent.service.TraceService;
import com.aftersales.platform.agent.service.ToolCallBudgetService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Agent 调用业务工具的统一入口：优先走 MCP 协议，连接故障时按配置回退本地工具。
 */
@Component
public class AgentToolGateway {
    private final McpToolClient mcpClient;
    private final HarnessProperties properties;
    private final TraceService traceService;
    private final ToolCallBudgetService toolCallBudget;
    private final OrderTools orderTools;
    private final ProductTools productTools;
    private final TicketTools ticketTools;

    public AgentToolGateway(McpToolClient mcpClient, HarnessProperties properties, TraceService traceService,
                            ToolCallBudgetService toolCallBudget, OrderTools orderTools,
                            ProductTools productTools, TicketTools ticketTools) {
        this.mcpClient = mcpClient;
        this.properties = properties;
        this.traceService = traceService;
        this.toolCallBudget = toolCallBudget;
        this.orderTools = orderTools;
        this.productTools = productTools;
        this.ticketTools = ticketTools;
    }

    public OrderInfo getLatestOrder(Long runId, Long userId) {
        return invoke(runId, "getLatestOrder", Map.of("userId", userId), OrderInfo.class,
                () -> orderTools.getLatestOrder(runId, userId));
    }

    public OrderInfo getOrder(Long runId, Long orderId) {
        return invoke(runId, "getOrder", Map.of("orderId", orderId), OrderInfo.class,
                () -> orderTools.getOrder(runId, orderId));
    }

    public Boolean isDelayedShipment(Long runId, Long orderId) {
        return invoke(runId, "isDelayedShipment", Map.of("orderId", orderId), Boolean.class,
                () -> orderTools.isDelayedShipment(runId, orderId));
    }

    public OrderInfo cancelOrder(Long runId, Long orderId, String reason) {
        return invoke(runId, "cancelOrder", Map.of("orderId", orderId, "reason", reason), OrderInfo.class,
                () -> orderTools.cancelOrder(runId, orderId, reason));
    }

    public List<Product> searchProducts(Long runId, String keyword) {
        return invoke(runId, "searchProducts", Map.of("keyword", keyword),
                new TypeReference<List<Product>>() { },
                () -> productTools.searchProducts(runId, keyword));
    }

    public Product getProduct(Long runId, Long productId) {
        return invoke(runId, "getProduct", Map.of("productId", productId), Product.class,
                () -> productTools.getProduct(runId, productId));
    }

    public String getAfterSalesPolicy(Long runId, Long productId) {
        return invoke(runId, "getAfterSalesPolicy", Map.of("productId", productId), String.class,
                () -> productTools.getAfterSalesPolicy(runId, productId));
    }

    public Ticket createTicket(Long runId, Long orderId, Long userId, Long productId,
                               String reason, String reply) {
        return invoke(runId, "createTicket",
                Map.of("orderId", orderId, "userId", userId, "productId", productId,
                        "reason", reason, "reply", reply),
                Ticket.class,
                () -> ticketTools.createTicket(runId, orderId, userId, productId, reason, reply));
    }

    public Ticket getTicket(Long runId, Long ticketId) {
        return invoke(runId, "getTicket", Map.of("ticketId", ticketId), Ticket.class,
                () -> ticketTools.getTicket(runId, ticketId));
    }

    public List<Ticket> getTicketsByOrder(Long runId, Long orderId) {
        return invoke(runId, "getTicketsByOrder", Map.of("orderId", orderId),
                new TypeReference<List<Ticket>>() { },
                () -> ticketTools.getTicketsByOrder(runId, orderId));
    }

    private <T> T invoke(Long runId, String toolName, Map<String, Object> arguments,
                         Class<T> resultType, Supplier<T> localCall) {
        toolCallBudget.acquire(runId);
        if (!properties.isMcpClientEnabled()) {
            return localCall.get();
        }
        try {
            return mcpClient.call(runId, toolName, arguments, resultType);
        } catch (McpToolClient.McpTransportException exception) {
            return fallback(runId, toolName, exception, localCall);
        }
    }

    private <T> T invoke(Long runId, String toolName, Map<String, Object> arguments,
                         TypeReference<T> resultType, Supplier<T> localCall) {
        toolCallBudget.acquire(runId);
        if (!properties.isMcpClientEnabled()) {
            return localCall.get();
        }
        try {
            return mcpClient.call(runId, toolName, arguments, resultType);
        } catch (McpToolClient.McpTransportException exception) {
            return fallback(runId, toolName, exception, localCall);
        }
    }

    private <T> T fallback(Long runId, String toolName, RuntimeException exception, Supplier<T> localCall) {
        if (!properties.isMcpFallbackEnabled()) {
            throw exception;
        }
        traceService.step(runId, "MCP Client", "tools/call " + toolName,
                "TRANSPORT_FAILED, fallback=LOCAL, error=" + exception.getMessage());
        return localCall.get();
    }
}
