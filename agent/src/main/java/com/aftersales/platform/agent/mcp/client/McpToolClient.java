package com.aftersales.platform.agent.mcp.client;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.service.TraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 懒加载的 MCP Streamable HTTP 客户端。
 * 首次业务调用时通过 Nacos 发现独立 Business MCP Server。
 */
@Component
public class McpToolClient {
    private static final Logger log = LoggerFactory.getLogger(McpToolClient.class);

    private final HarnessProperties properties;
    private final ObjectMapper objectMapper;
    private final TraceService traceService;
    private final BusinessMcpEndpointResolver endpointResolver;
    private volatile McpSyncClient client;

    public McpToolClient(HarnessProperties properties, ObjectMapper objectMapper, TraceService traceService,
                         BusinessMcpEndpointResolver endpointResolver) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.traceService = traceService;
        this.endpointResolver = endpointResolver;
    }

    public <T> T call(Long runId, String toolName, Map<String, Object> arguments, Class<T> resultType) {
        return call(runId, toolName, arguments, objectMapper.getTypeFactory().constructType(resultType));
    }

    public <T> T call(Long runId, String toolName, Map<String, Object> arguments, TypeReference<T> resultType) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(resultType);
        return call(runId, toolName, arguments, javaType);
    }

    private <T> T call(Long runId, String toolName, Map<String, Object> arguments, JavaType resultType) {
        Map<String, Object> requestArguments = new LinkedHashMap<>(arguments);
        requestArguments.put("runId", runId);

        McpSchema.CallToolResult result;
        long startedAt = System.nanoTime();
        try {
            result = client().callTool(new McpSchema.CallToolRequest(toolName, requestArguments));
        } catch (RuntimeException exception) {
            resetClient();
            throw new McpTransportException("MCP tools/call 失败: " + exception.getMessage(), exception);
        }

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("MCP 工具执行失败: " + contentText(result));
        }

        T value = deserialize(result, resultType);
        traceService.step(runId, "MCP Client", "tools/call " + toolName,
                "SUCCESS, elapsedMs=" + elapsedMs);
        return value;
    }

    private synchronized McpSyncClient client() {
        if (client != null && client.isInitialized()) {
            return client;
        }

        URI configuredUri = endpointResolver.resolve();
        String baseUrl = configuredUri.getScheme() + "://" + configuredUri.getAuthority();
        String endpoint = configuredUri.getPath();
        if (endpoint == null || endpoint.isBlank() || "/".equals(endpoint)) {
            endpoint = "/mcp";
        }

        Duration timeout = Duration.ofSeconds(properties.getMcpRequestTimeoutSeconds());
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .connectTimeout(timeout)
                .build();

        McpSyncClient newClient = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("aftersales-agent-harness", "1.0.0"))
                .initializationTimeout(timeout)
                .requestTimeout(timeout)
                .build();
        newClient.initialize();

        int toolCount = newClient.listTools().tools().size();
        log.info("MCP_CLIENT_CONNECTED url={} tools={}", configuredUri, toolCount);
        client = newClient;
        return newClient;
    }

    private <T> T deserialize(McpSchema.CallToolResult result, JavaType resultType) {
        if (result.structuredContent() != null) {
            return objectMapper.convertValue(result.structuredContent(), resultType);
        }

        String text = contentText(result);
        try {
            return objectMapper.readValue(text, resultType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析 MCP 工具返回值", exception);
        }
    }

    private String contentText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .findFirst()
                .orElse("");
    }

    private synchronized void resetClient() {
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (RuntimeException ignored) {
                // 连接已经失效，无需覆盖原异常。
            }
            client = null;
        }
    }

    @PreDestroy
    public void close() {
        resetClient();
    }

    public static class McpTransportException extends RuntimeException {
        public McpTransportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
