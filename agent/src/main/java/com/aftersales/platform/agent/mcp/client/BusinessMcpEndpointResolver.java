package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * Business MCP 地址解析器，优先使用显式地址，否则通过 Nacos 发现可用实例。
 */
@Component
public class BusinessMcpEndpointResolver {
    private final DiscoveryClient discovery;
    private final HarnessProperties properties;

    public BusinessMcpEndpointResolver(DiscoveryClient discovery, HarnessProperties properties) {
        this.discovery = discovery;
        this.properties = properties;
    }

    public URI resolve() {
        // ===== 1) 显式 MCP URL 适合本地调试，并且优先级最高 =====
        if (properties.getBusinessMcpUrl() != null && !properties.getBusinessMcpUrl().isBlank()) {
            return URI.create(properties.getBusinessMcpUrl());
        }
        // ===== 2) 未显式配置时按服务名从 Nacos 获取可用 Business 实例 =====
        List<ServiceInstance> instances = discovery.getInstances(properties.getBusinessServiceName());
        if (!instances.isEmpty()) {
            return instances.getFirst().getUri();
        }
        // ===== 3) Nacos 暂时无实例时使用本地兜底地址，下一次懒加载仍会重新解析 =====
        return URI.create("http://localhost:8070");
    }
}
