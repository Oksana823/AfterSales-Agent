package com.aftersales.platform.business.config;

import com.aftersales.platform.business.mcp.OrderTools;
import com.aftersales.platform.business.mcp.ProductTools;
import com.aftersales.platform.business.mcp.TicketTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfiguration {
    @Bean ToolCallbackProvider afterSalesTools(OrderTools orders, ProductTools products, TicketTools tickets) {
        return MethodToolCallbackProvider.builder().toolObjects(orders, products, tickets).build();
    }
}
