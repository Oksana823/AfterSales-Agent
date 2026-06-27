package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.service.ToolCallBudgetService;
import com.aftersales.platform.common.domain.OrderInfo;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentToolGatewayTest {
    @Test void delegatesToRemoteMcpAndConsumesBudget() {
        McpToolClient client = mock(McpToolClient.class); HarnessProperties properties = new HarnessProperties(); ToolCallBudgetService budget = mock(ToolCallBudgetService.class); AgentToolGateway gateway = new AgentToolGateway(client, properties, budget);
        OrderInfo expected = mock(OrderInfo.class); when(client.call(1L, "getOrder", Map.of("orderId", 10001L), OrderInfo.class)).thenReturn(expected);
        assertThat(gateway.getOrder(1L, 10001L)).isSameAs(expected); verify(budget).acquire(1L);
    }
    @Test void disabledClientCannotBypassBusinessService() {
        HarnessProperties properties = new HarnessProperties(); properties.setMcpClientEnabled(false); AgentToolGateway gateway = new AgentToolGateway(mock(McpToolClient.class), properties, mock(ToolCallBudgetService.class));
        assertThatThrownBy(() -> gateway.getOrder(1L, 1L)).hasMessageContaining("已关闭");
    }
}
