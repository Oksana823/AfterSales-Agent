package com.aftersales.platform.agent.agent;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.service.ModelCallTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AiModelService {
    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final HarnessProperties properties;
    private final ModelCallTraceService traceService;
    private final String modelName;

    public AiModelService(ObjectProvider<ChatModel> chatModelProvider, HarnessProperties properties,
                          ModelCallTraceService traceService,
                          @Value("#{environment.getProperty('spring.ai.openai.chat.options.model', 'unknown')}") String modelName) {
        this.chatModelProvider = chatModelProvider;
        this.properties = properties;
        this.traceService = traceService;
        this.modelName = modelName;
    }

    /**
     * 返回结构化模型状态，调用方必须显式决定失败后的场景策略。
     */
    public ModelResult generate(Long runId, String scene, String systemPrompt, String userPrompt) {
        if (!properties.isLlmEnabled()) {
            return failure(runId, scene, ModelStatus.DISABLED, 0, null);
        }
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return failure(runId, scene, ModelStatus.UNAVAILABLE, 0, "ChatModel unavailable");
        }

        long startedAt = System.nanoTime();
        log.info("LLM_CALL_START runId={} scene={}", runId, scene);
        try {
            String content = ChatClient.builder(chatModel).build().prompt()
                    .system(systemPrompt).user(userPrompt).call().content();
            long elapsed = elapsedMs(startedAt);
            if (content == null || content.isBlank()) {
                return failure(runId, scene, ModelStatus.EMPTY_RESPONSE, elapsed, null);
            }
            traceService.record(runId, scene, modelName, elapsed, ModelStatus.SUCCESS.name(), null);
            log.info("LLM_CALL_SUCCESS runId={} scene={} elapsedMs={}", runId, scene, elapsed);
            return new ModelResult(ModelStatus.SUCCESS, content.trim());
        } catch (RuntimeException exception) {
            long elapsed = elapsedMs(startedAt);
            ModelStatus status = classify(exception);
            log.warn("LLM_CALL_FAILED runId={} scene={} status={} elapsedMs={}", runId, scene, status, elapsed);
            return failure(runId, scene, status, elapsed, sanitize(exception.getMessage()));
        }
    }

    public void recordInvalidOutput(Long runId, String scene) {
        traceService.record(runId, scene, modelName, 0, ModelStatus.INVALID_OUTPUT.name(), "模型输出格式不符合约束");
    }

    private ModelResult failure(Long runId, String scene, ModelStatus status, long elapsed, String error) {
        traceService.record(runId, scene, modelName, elapsed, status.name(), error);
        return new ModelResult(status, null);
    }

    private ModelStatus classify(RuntimeException exception) {
        String message = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
        if (message.contains("401") || message.contains("403") || message.contains("authentication")) {
            return ModelStatus.AUTH_FAILED;
        }
        if (message.contains("429") || message.contains("rate limit")) {
            return ModelStatus.RATE_LIMITED;
        }
        if (message.contains("timeout") || message.contains("timed out")) {
            return ModelStatus.TIMEOUT;
        }
        return ModelStatus.SERVER_ERROR;
    }

    private String sanitize(String message) {
        if (message == null) {
            return null;
        }
        String safe = message.replaceAll("(?i)(api\s*key\s*[:=]\s*)[^, }]+", "$1***");
        return safe.length() > 500 ? safe.substring(0, 500) : safe;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    public enum ModelStatus {
        SUCCESS, DISABLED, UNAVAILABLE, AUTH_FAILED, RATE_LIMITED, TIMEOUT, SERVER_ERROR,
        EMPTY_RESPONSE, INVALID_OUTPUT
    }

    public record ModelResult(ModelStatus status, String content) {
        public boolean success() {
            return status == ModelStatus.SUCCESS;
        }
    }
}
