package com.dominikcebula.spring.ai.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "harness")
public class HarnessProperties {
    private String mcpServerUrl;
    private boolean mcpClientEnabled = true;
    private boolean mcpFallbackEnabled = true;
    private int mcpRequestTimeoutSeconds = 10;
    private int maxToolCalls = 12;
    private boolean distributedAgentsEnabled = true;
    private String agentWorkerUrl = "http://aftersales-agent-worker";
    private boolean approvalEnabled = true;
    private int delayedShipmentThresholdHours = 48;
    private long redisCacheSeconds = 600;
    private String elasticsearchIndexName = "aftersales_products";
    private String elasticsearchUrl = "http://localhost:9200";
    private boolean llmEnabled = false;
    public String getMcpServerUrl() { return mcpServerUrl; }
    public void setMcpServerUrl(String mcpServerUrl) { this.mcpServerUrl = mcpServerUrl; }
    public boolean isMcpClientEnabled() { return mcpClientEnabled; }
    public void setMcpClientEnabled(boolean mcpClientEnabled) { this.mcpClientEnabled = mcpClientEnabled; }
    public boolean isMcpFallbackEnabled() { return mcpFallbackEnabled; }
    public void setMcpFallbackEnabled(boolean mcpFallbackEnabled) { this.mcpFallbackEnabled = mcpFallbackEnabled; }
    public int getMcpRequestTimeoutSeconds() { return mcpRequestTimeoutSeconds; }
    public void setMcpRequestTimeoutSeconds(int mcpRequestTimeoutSeconds) { this.mcpRequestTimeoutSeconds = mcpRequestTimeoutSeconds; }
    public boolean isDistributedAgentsEnabled() { return distributedAgentsEnabled; }
    public void setDistributedAgentsEnabled(boolean distributedAgentsEnabled) { this.distributedAgentsEnabled = distributedAgentsEnabled; }
    public String getAgentWorkerUrl() { return agentWorkerUrl; }
    public void setAgentWorkerUrl(String agentWorkerUrl) { this.agentWorkerUrl = agentWorkerUrl; }
    public int getMaxToolCalls() { return maxToolCalls; }
    public void setMaxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; }
    public boolean isApprovalEnabled() { return approvalEnabled; }
    public void setApprovalEnabled(boolean approvalEnabled) { this.approvalEnabled = approvalEnabled; }
    public int getDelayedShipmentThresholdHours() { return delayedShipmentThresholdHours; }
    public void setDelayedShipmentThresholdHours(int delayedShipmentThresholdHours) { this.delayedShipmentThresholdHours = delayedShipmentThresholdHours; }
    public long getRedisCacheSeconds() { return redisCacheSeconds; }
    public void setRedisCacheSeconds(long redisCacheSeconds) { this.redisCacheSeconds = redisCacheSeconds; }
    public String getElasticsearchIndexName() { return elasticsearchIndexName; }
    public void setElasticsearchIndexName(String elasticsearchIndexName) { this.elasticsearchIndexName = elasticsearchIndexName; }
    public String getElasticsearchUrl() { return elasticsearchUrl; }
    public void setElasticsearchUrl(String elasticsearchUrl) { this.elasticsearchUrl = elasticsearchUrl; }
    public boolean isLlmEnabled() { return llmEnabled; }
    public void setLlmEnabled(boolean llmEnabled) { this.llmEnabled = llmEnabled; }
}
