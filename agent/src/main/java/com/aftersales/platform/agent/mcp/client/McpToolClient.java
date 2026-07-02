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
        // ===== 1) 组装 MCP 参数：业务参数之外统一附带 runId，供 Business 侧写工具调用日志 =====
        Map<String, Object> requestArguments = new LinkedHashMap<>(arguments);
        requestArguments.put("runId", runId);

        // ===== 2) 发起 tools/call，并统计从 Agent 到 MCP Server 的调用耗时 =====
        McpSchema.CallToolResult result;
        long startedAt = System.nanoTime();
        try {
            result = client().callTool(new McpSchema.CallToolRequest(toolName, requestArguments));
        } catch (RuntimeException exception) {
            resetClient();
            throw new McpTransportException("MCP tools/call 失败: " + exception.getMessage(), exception);
        }

        // ===== 3) 区分协议成功和业务失败：isError=true 时保留 Business 返回的真实错误 =====
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("MCP 工具执行失败: " + contentText(result));
        }

        // ===== 4) 将 MCP 内容转换成调用方需要的 Java 类型，并把成功步骤写入 Trace =====
        T value = deserialize(result, resultType);
        traceService.step(runId, "MCP Client", "tools/call " + toolName,
                "SUCCESS, elapsedMs=" + elapsedMs);
        return value;
    }

    private synchronized McpSyncClient client() {
        // ===== 1) 已完成握手的客户端直接复用，避免每次工具调用都重新建连 =====
        if (client != null && client.isInitialized()) {
            return client;
        }

        // ===== 2) 解析 Business MCP 地址：显式配置优先，否则通过 Nacos 服务发现 =====
        URI configuredUri = endpointResolver.resolve();
        String baseUrl = configuredUri.getScheme() + "://" + configuredUri.getAuthority();
        String endpoint = configuredUri.getPath();
        if (endpoint == null || endpoint.isBlank() || "/".equals(endpoint)) {
            endpoint = "/mcp";
        }

        // ===== 3) 构建 Streamable HTTP 传输层，连接和请求超时统一取 Nacos 配置 =====
        Duration timeout = Duration.ofSeconds(properties.getMcpRequestTimeoutSeconds());
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .connectTimeout(timeout)
                .build();

        // ===== 4) 构建同步 MCP Client，并声明当前客户端身份和协议超时 =====
        McpSyncClient newClient = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("aftersales-agent-harness", "1.0.0"))
                .initializationTimeout(timeout)
                .requestTimeout(timeout)
                .build();
        // ===== 5) 执行 MCP initialize 握手，完成协议版本与能力协商 =====
        newClient.initialize();

        // ===== 6) 主动列出工具验证连接，随后才发布到 volatile 字段供其他线程复用 =====
        int toolCount = newClient.listTools().tools().size();
        log.info("MCP_CLIENT_CONNECTED url={} tools={}", configuredUri, toolCount);
        client = newClient;
        return newClient;
    }

    private <T> T deserialize(McpSchema.CallToolResult result, JavaType resultType) {
        // ===== 优先读取 MCP structuredContent，避免对结构化结果做二次字符串解析 =====
        if (result.structuredContent() != null) {
            return objectMapper.convertValue(result.structuredContent(), resultType);
        }

        // ===== 兼容只返回 TextContent 的 MCP Server，再用 Jackson 反序列化 =====
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
