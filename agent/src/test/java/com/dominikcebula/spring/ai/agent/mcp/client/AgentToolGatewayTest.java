package com.dominikcebula.spring.ai.agent.mcp.client;

import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import com.dominikcebula.spring.ai.agent.domain.Enums.OrderStatus;
import com.dominikcebula.spring.ai.agent.domain.OrderInfo;
import com.dominikcebula.spring.ai.agent.mcp.OrderTools;
import com.dominikcebula.spring.ai.agent.mcp.ProductTools;
import com.dominikcebula.spring.ai.agent.mcp.TicketTools;
import com.dominikcebula.spring.ai.agent.service.TraceService;
import com.dominikcebula.spring.ai.agent.service.ToolCallBudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentToolGatewayTest {
    private final McpToolClient mcpClient = mock(McpToolClient.class);
    private final TraceService traceService = mock(TraceService.class);
    private final ToolCallBudgetService toolCallBudget = mock(ToolCallBudgetService.class);
    private final OrderTools orderTools = mock(OrderTools.class);
    private final ProductTools productTools = mock(ProductTools.class);
    private final TicketTools ticketTools = mock(TicketTools.class);
    private final HarnessProperties properties = new HarnessProperties();
    private AgentToolGateway gateway;

    @BeforeEach
    void setUp() {
        properties.setMcpClientEnabled(true);
        properties.setMcpFallbackEnabled(true);
        gateway = new AgentToolGateway(
                mcpClient, properties, traceService, toolCallBudget, orderTools, productTools, ticketTools
        );
    }

    @Test
    void transportFailureFallsBackToLocalTool() {
        OrderInfo order = order();
        when(mcpClient.call(1L, "getOrder", Map.of("orderId", 10001L), OrderInfo.class))
                .thenThrow(new McpToolClient.McpTransportException("connection refused", null));
        when(orderTools.getOrder(1L, 10001L)).thenReturn(order);

        assertThat(gateway.getOrder(1L, 10001L)).isEqualTo(order);
        verify(orderTools).getOrder(1L, 10001L);
        verify(traceService).step(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("MCP Client"),
                org.mockito.ArgumentMatchers.eq("tools/call getOrder"),
                org.mockito.ArgumentMatchers.contains("fallback=LOCAL")
        );
    }

    @Test
    void businessFailureDoesNotRetryLocally() {
        when(mcpClient.call(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("getOrder"),
                anyMap(),
                org.mockito.ArgumentMatchers.eq(OrderInfo.class)
        )).thenThrow(new IllegalStateException("订单不存在"));

        assertThatThrownBy(() -> gateway.getOrder(1L, 99999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("订单不存在");
        verifyNoInteractions(orderTools);
    }

    private OrderInfo order() {
        LocalDateTime now = LocalDateTime.now();
        return new OrderInfo(10001L, 10086L, 1L, OrderStatus.PAID, now, now, null, null);
    }
}
