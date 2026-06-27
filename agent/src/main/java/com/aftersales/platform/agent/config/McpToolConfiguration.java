package com.aftersales.platform.agent.config;

import com.aftersales.platform.agent.mcp.OrderTools;
import com.aftersales.platform.agent.mcp.ProductTools;
import com.aftersales.platform.agent.mcp.TicketTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将业务工具注册为 Spring AI ToolCallback，并由 MCP Server 自动暴露。
 */
@Configuration
public class McpToolConfiguration {

    @Bean
    public ToolCallbackProvider afterSalesTools(
            OrderTools orderTools,
            ProductTools productTools,
            TicketTools ticketTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderTools, productTools, ticketTools)
                .build();
    }
}
