package com.dominikcebula.spring.ai.agent.service;

import com.dominikcebula.spring.ai.agent.config.HarnessProperties;
import com.dominikcebula.spring.ai.agent.domain.Ticket;
import com.dominikcebula.spring.ai.agent.repository.TicketRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {
    private final TicketRepository repository;
    private final StringRedisTemplate redis;
    private final HarnessProperties properties;
    public TicketService(TicketRepository repository, StringRedisTemplate redis, HarnessProperties properties) { this.repository = repository; this.redis = redis; this.properties = properties; }
    public Ticket create(Long orderId, Long userId, Long productId, String reason, String reply) {
        String key = "idem:ticket:order:" + orderId;
        Boolean locked = redis.opsForValue().setIfAbsent(key, "1", properties.getRedisCacheSeconds(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return repository.findByOrderId(orderId).stream().findFirst().orElseThrow(() -> new IllegalStateException("工单创建中，请稍后查询"));
        }
        return repository.create(orderId, userId, productId, reason, reply);
    }
    public Ticket get(Long ticketId) { return repository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("工单不存在")); }
    public List<Ticket> byOrder(Long orderId) { return repository.findByOrderId(orderId); }
}
