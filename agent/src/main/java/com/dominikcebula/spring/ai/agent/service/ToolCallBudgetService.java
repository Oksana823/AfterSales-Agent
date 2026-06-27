package com.dominikcebula.spring.ai.agent.service;

import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ToolCallBudgetService {
    private static final Duration COUNTER_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;
    private final HarnessProperties properties;

    public ToolCallBudgetService(StringRedisTemplate redis, HarnessProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 每个 run 在调用工具前原子计数，超过配置上限立即终止。 */
    public void acquire(Long runId) {
        int limit = properties.getMaxToolCalls();
        if (limit <= 0) {
            return;
        }

        String key = "agent:run:" + runId + ":tool-calls";
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, COUNTER_TTL);
        }
        if (count != null && count > limit) {
            throw new IllegalStateException("工具调用次数超过上限：" + limit);
        }
    }
}
