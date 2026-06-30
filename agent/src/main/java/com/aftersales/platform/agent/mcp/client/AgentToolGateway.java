package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.service.ToolCallBudgetService;
import com.aftersales.platform.common.domain.OrderInfo;
import com.aftersales.platform.common.domain.Product;
import com.aftersales.platform.common.domain.Ticket;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agent 调用独立 Business MCP Server 的唯一业务入口。
 */
@Component
public class AgentToolGateway {
    private final McpToolClient mcpClient;
    private final HarnessProperties properties;
    private final ToolCallBudgetService budget;

    public AgentToolGateway(McpToolClient mcpClient, HarnessProperties properties, ToolCallBudgetService budget) {
        this.mcpClient = mcpClient;
        this.properties = properties;
        this.budget = budget;
    }

    public OrderInfo getLatestOrder(Long runId, Long userId) {
        return invoke(runId, "getLatestOrder", Map.of("userId", userId), OrderInfo.class);
    }

    public OrderInfo getOrder(Long runId, Long orderId) {
        return invoke(runId, "getOrder", Map.of("orderId", orderId), OrderInfo.class);
    }

    public Boolean isDelayedShipment(Long runId, Long orderId) {
        return invoke(runId, "isDelayedShipment", Map.of("orderId", orderId), Boolean.class);
    }

    public OrderInfo cancelOrder(Long runId, Long orderId, String reason) {
        return invoke(runId, "cancelOrder", Map.of("orderId", orderId, "reason", reason), OrderInfo.class);
    }

    public List<Product> searchProducts(Long runId, String keyword) {
        return invoke(runId, "searchProducts", Map.of("keyword", keyword), new TypeReference<List<Product>>() {
        });
    }

    public Product getProduct(Long runId, Long productId) {
        return invoke(runId, "getProduct", Map.of("productId", productId), Product.class);
    }

    public String getAfterSalesPolicy(Long runId, Long productId) {
        return invoke(runId, "getAfterSalesPolicy", Map.of("productId", productId), String.class);
    }

    public Ticket createTicket(Long runId, Long orderId, Long userId, Long productId, String reason, String reply) {
        return invoke(runId, "createTicket",
                Map.of("orderId", orderId, "userId", userId, "productId", productId, "reason", reason, "reply", reply),
                Ticket.class);
    }

    public Ticket updateTicketCustomerReply(Long runId, Long ticketId, String reply) {
        return invoke(runId, "updateTicketCustomerReply", Map.of("ticketId", ticketId, "reply", reply), Ticket.class);
    }

    public Ticket getTicket(Long runId, Long ticketId) {
        return invoke(runId, "getTicket", Map.of("ticketId", ticketId), Ticket.class);
    }

    public List<Ticket> getTicketsByOrder(Long runId, Long orderId) {
        return invoke(runId, "getTicketsByOrder", Map.of("orderId", orderId), new TypeReference<List<Ticket>>() {
        });
    }

    private <T> T invoke(Long runId, String name, Map<String, Object> args, Class<T> type) {
        ensureEnabled();
        budget.acquire(runId);
        return mcpClient.call(runId, name, args, type);
    }

    private <T> T invoke(Long runId, String name, Map<String, Object> args, TypeReference<T> type) {
        ensureEnabled();
        budget.acquire(runId);
        return mcpClient.call(runId, name, args, type);
    }

    private void ensureEnabled() {
        if (!properties.isMcpClientEnabled()) {
            throw new IllegalStateException("Business MCP Client 已关闭");
        }
    }
}
