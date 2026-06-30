package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
public class BusinessMcpEndpointResolver {
    private final DiscoveryClient discovery;
    private final HarnessProperties properties;

    public BusinessMcpEndpointResolver(DiscoveryClient discovery, HarnessProperties properties) {
        this.discovery = discovery;
        this.properties = properties;
    }

    public URI resolve() {
        if (properties.getBusinessMcpUrl() != null && !properties.getBusinessMcpUrl().isBlank()) {
            return URI.create(properties.getBusinessMcpUrl());
        }
        List<ServiceInstance> instances = discovery.getInstances(properties.getBusinessServiceName());
        if (!instances.isEmpty()) {
            return instances.getFirst().getUri();
        }
        return URI.create("http://localhost:8070");
    }
}
