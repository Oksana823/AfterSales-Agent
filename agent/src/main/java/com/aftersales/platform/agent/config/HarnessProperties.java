package com.aftersales.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent Harness 配置模型，集中承载 MCP、Worker、LLM 和工具预算等可变参数。
 */
@ConfigurationProperties(prefix = "harness")
public class HarnessProperties {
    // Nacos 中注册的 Business 服务名，用于发现 MCP Server 实例。
    private String businessServiceName = "aftersales-business";
    // 可选的固定 MCP 地址；配置后跳过 Nacos，适合本地调试。
    private String businessMcpUrl;
    // MCP 总开关和单次请求超时。
    private boolean mcpClientEnabled = true;
    private int mcpRequestTimeoutSeconds = 10;
    // 单个 Run 最多可调用的工具次数，Redis 负责原子计数。
    private int maxToolCalls = 12;
    // 是否把角色调用分发到独立 Worker，以及 Worker 的负载均衡地址。
    private boolean distributedAgentsEnabled = true;
    private String agentWorkerUrl = "http://aftersales-agent-worker";
    // LLM 总开关；关闭后 Supervisor/Planner/Reporter 按各自策略降级。
    private boolean llmEnabled = false;

    public String getBusinessServiceName() {
        return businessServiceName;
    }

    public void setBusinessServiceName(String value) {
        this.businessServiceName = value;
    }

    public String getBusinessMcpUrl() {
        return businessMcpUrl;
    }

    public void setBusinessMcpUrl(String value) {
        this.businessMcpUrl = value;
    }

    public boolean isMcpClientEnabled() {
        return mcpClientEnabled;
    }

    public void setMcpClientEnabled(boolean value) {
        this.mcpClientEnabled = value;
    }

    public int getMcpRequestTimeoutSeconds() {
        return mcpRequestTimeoutSeconds;
    }

    public void setMcpRequestTimeoutSeconds(int value) {
        this.mcpRequestTimeoutSeconds = value;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int value) {
        this.maxToolCalls = value;
    }

    public boolean isDistributedAgentsEnabled() {
        return distributedAgentsEnabled;
    }

    public void setDistributedAgentsEnabled(boolean value) {
        this.distributedAgentsEnabled = value;
    }

    public String getAgentWorkerUrl() {
        return agentWorkerUrl;
    }

    public void setAgentWorkerUrl(String value) {
        this.agentWorkerUrl = value;
    }

    public boolean isLlmEnabled() {
        return llmEnabled;
    }

    public void setLlmEnabled(boolean value) {
        this.llmEnabled = value;
    }
}
