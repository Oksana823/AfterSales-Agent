package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.business.repository.TicketRepository;
import com.aftersales.platform.common.domain.Ticket;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 售后工单业务服务，提供 Redis 幂等创建、详情查询和客服回复回写。
 */
@Service
public class TicketService {
    private final TicketRepository repository;
    private final StringRedisTemplate redis;
    private final BusinessProperties properties;

    public TicketService(TicketRepository repository, StringRedisTemplate redis, BusinessProperties properties) {
        this.repository = repository;
        this.redis = redis;
        this.properties = properties;
    }

    public Ticket create(Long orderId, Long userId, Long productId, String reason, String reply) {
        // ===== 1) 以订单 ID 作为幂等键，同一订单在 TTL 内只允许一个创建者 =====
        String key = "idem:ticket:order:" + orderId;
        Boolean locked = redis.opsForValue().setIfAbsent(key, "1", properties.getRedisCacheSeconds(), TimeUnit.SECONDS);
        // ===== 2) 未抢到幂等锁时直接返回已存在工单，避免重复插入 =====
        if (Boolean.FALSE.equals(locked)) {
            return repository.findByOrderId(orderId).stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("工单创建中，请稍后查询"));
        }
        // ===== 3) 只有成功获得 Redis 幂等锁的请求才能真正写 MySQL =====
        return repository.create(orderId, userId, productId, reason, reply);
    }

    public Ticket updateCustomerReply(Long ticketId, String reply) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("客服回复不能为空");
        }
        if (repository.updateCustomerReply(ticketId, reply) != 1) {
            throw new IllegalStateException("工单回复更新失败");
        }
        return get(ticketId);
    }

    public Ticket get(Long ticketId) {
        return repository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("工单不存在"));
    }

    public List<Ticket> byOrder(Long orderId) {
        return repository.findByOrderId(orderId);
    }
}
