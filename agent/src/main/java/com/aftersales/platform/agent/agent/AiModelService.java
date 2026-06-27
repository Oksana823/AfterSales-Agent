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

import java.util.Optional;

@Component
public class AiModelService {
    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final HarnessProperties properties;
    private final ModelCallTraceService traceService;
    private final String modelName;

    public AiModelService(ObjectProvider<ChatModel> chatModelProvider,
                          HarnessProperties properties,
                          ModelCallTraceService traceService,
                          @Value("#{environment.getProperty('spring.ai.openai.chat.options.model', 'unknown')}")
                          String modelName) {
        this.chatModelProvider = chatModelProvider;
        this.properties = properties;
        this.traceService = traceService;
        this.modelName = modelName;
    }

    /** 记录模型、场景、耗时和状态；失败后返回 empty 触发规则回退。 */
    public Optional<String> generate(Long runId, String scene, String systemPrompt, String userPrompt) {
        if (!properties.isLlmEnabled()) {
            return Optional.empty();
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            traceService.record(runId, scene, modelName, 0, "FAILED", "ChatModel unavailable");
            return Optional.empty();
        }

        long startedAt = System.nanoTime();
        log.info("LLM_CALL_START runId={} scene={}", runId, scene);
        try {
            String content = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            long elapsedMs = elapsedMs(startedAt);
            if (content == null || content.isBlank()) {
                traceService.record(runId, scene, modelName, elapsedMs, "EMPTY", null);
                return Optional.empty();
            }
            traceService.record(runId, scene, modelName, elapsedMs, "SUCCESS", null);
            log.info("LLM_CALL_SUCCESS runId={} scene={} elapsedMs={}", runId, scene, elapsedMs);
            return Optional.of(content.trim());
        } catch (RuntimeException exception) {
            long elapsedMs = elapsedMs(startedAt);
            traceService.record(runId, scene, modelName, elapsedMs, "FAILED", exception.getMessage());
            log.warn("LLM_CALL_FAILED runId={} scene={} elapsedMs={} fallback=rules",
                    runId, scene, elapsedMs);
            return Optional.empty();
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
