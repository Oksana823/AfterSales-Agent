package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.common.domain.Enums.OrderStatus;
import com.aftersales.platform.common.domain.OrderInfo;
import com.aftersales.platform.business.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final BusinessProperties properties;
    private final StringRedisTemplate redis;
    private final JsonService json;
    private final CancellationAuthorizationService cancellationAuthorization;

    public OrderService(OrderRepository repository, BusinessProperties properties,
                        StringRedisTemplate redis, JsonService json,
                        CancellationAuthorizationService cancellationAuthorization) {
        this.repository = repository;
        this.properties = properties;
        this.redis = redis;
        this.json = json;
        this.cancellationAuthorization = cancellationAuthorization;
    }

    public OrderInfo latestOrder(Long userId) {
        return repository.findLatestByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户没有订单"));
    }

    public OrderInfo getOrder(Long orderId) {
        OrderInfo order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        redis.opsForValue().set(
                "cache:order:" + orderId,
                json.toJson(order),
                properties.getRedisCacheSeconds(),
                TimeUnit.SECONDS
        );
        return order;
    }

    public boolean isDelayedShipment(Long orderId) {
        OrderInfo order = getOrder(orderId);
        return order.status() == OrderStatus.PAID
                && order.shippedAt() == null
                && order.paidAt() != null
                && order.paidAt().plusHours(properties.getDelayedShipmentThresholdHours())
                .isBefore(LocalDateTime.now());
    }

    /**
     * 取消操作同时校验订单状态和一次性审批授权。
     * 已取消订单按幂等请求返回，其他不可取消状态直接拒绝。
     */
    public OrderInfo cancelOrder(Long runId, Long orderId, String reason) {
        OrderInfo current = getOrder(orderId);
        if (current.status() == OrderStatus.CANCELLED) {
            return current;
        }
        if (current.status() != OrderStatus.CREATED && current.status() != OrderStatus.PAID) {
            throw new IllegalStateException("订单状态" + current.status() + "不允许取消");
        }

        cancellationAuthorization.consume(runId, orderId);
        int updated = repository.cancel(orderId, reason);
        if (updated != 1) {
            throw new IllegalStateException("订单状态已变化，取消失败");
        }

        redis.delete("cache:order:" + orderId);
        return getOrder(orderId);
    }
}
