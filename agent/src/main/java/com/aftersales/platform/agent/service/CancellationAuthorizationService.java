package com.aftersales.platform.agent.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 为审批通过后的取消操作签发一次性授权，防止外部 MCP Client 绕过审批直接调用 cancelOrder。
 */
@Service
public class CancellationAuthorizationService {
    private static final Duration AUTHORIZATION_TTL = Duration.ofMinutes(2);
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; "
                    + "return value;",
            String.class
    );

    private final StringRedisTemplate redis;

    public CancellationAuthorizationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void authorize(Long runId, Long orderId, Long approvalId) {
        redis.opsForValue().set(key(runId, orderId), approvalId.toString(), AUTHORIZATION_TTL);
    }

    public void consume(Long runId, Long orderId) {
        String approvalId = redis.execute(CONSUME_SCRIPT, List.of(key(runId, orderId)));
        if (approvalId == null) {
            throw new IllegalStateException("取消订单缺少有效审批授权");
        }
    }

    public void revoke(Long runId, Long orderId) {
        redis.delete(key(runId, orderId));
    }

    private String key(Long runId, Long orderId) {
        return "approval:cancel:run:" + runId + ":order:" + orderId;
    }
}
