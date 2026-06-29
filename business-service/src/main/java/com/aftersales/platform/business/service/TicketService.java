package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.business.repository.TicketRepository;
import com.aftersales.platform.common.domain.Ticket;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {
    private final TicketRepository repository; private final StringRedisTemplate redis; private final BusinessProperties properties;
    public TicketService(TicketRepository repository, StringRedisTemplate redis, BusinessProperties properties) { this.repository = repository; this.redis = redis; this.properties = properties; }
    public Ticket create(Long orderId, Long userId, Long productId, String reason, String reply) {
        String key = "idem:ticket:order:" + orderId;
        Boolean locked = redis.opsForValue().setIfAbsent(key, "1", properties.getRedisCacheSeconds(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) return repository.findByOrderId(orderId).stream().findFirst().orElseThrow(() -> new IllegalStateException("工单创建中，请稍后查询"));
        return repository.create(orderId, userId, productId, reason, reply);
    }
    public Ticket updateCustomerReply(Long ticketId, String reply) {
        if (reply == null || reply.isBlank()) throw new IllegalArgumentException("客服回复不能为空");
        if (repository.updateCustomerReply(ticketId, reply) != 1) throw new IllegalStateException("工单回复更新失败");
        return get(ticketId);
    }
    public Ticket get(Long ticketId) { return repository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("工单不存在")); }
    public List<Ticket> byOrder(Long orderId) { return repository.findByOrderId(orderId); }
}
