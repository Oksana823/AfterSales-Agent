package com.aftersales.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "harness")
public class HarnessProperties {
    private String businessServiceName = "aftersales-business";
    private String businessMcpUrl;
    private boolean mcpClientEnabled = true;
    private int mcpRequestTimeoutSeconds = 10;
    private int maxToolCalls = 12;
    private boolean distributedAgentsEnabled = true;
    private String agentWorkerUrl = "http://aftersales-agent-worker";
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
