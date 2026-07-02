package com.aftersales.platform.business.service;

import com.aftersales.platform.business.config.BusinessProperties;
import com.aftersales.platform.common.domain.Enums.OrderStatus;
import com.aftersales.platform.common.domain.OrderInfo;
import com.aftersales.platform.business.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 订单业务服务，负责订单查询、延迟判断和带审批授权的状态变更。
 */
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
        // ===== 订单以 MySQL 为事实来源，查询成功后写入 Redis 详情缓存 =====
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
        // ===== 延迟发货必须同时满足已付款、未发货、存在付款时间且超过配置阈值 =====
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
        // ===== 1) 取消前重新读取订单，不能依赖 Agent 早先查询到的旧状态 =====
        OrderInfo current = getOrder(orderId);
        if (current.status() == OrderStatus.CANCELLED) {
            return current;
        }
        if (current.status() != OrderStatus.CREATED && current.status() != OrderStatus.PAID) {
            throw new IllegalStateException("订单状态" + current.status() + "不允许取消");
        }

        // ===== 2) 原子消费审批凭证：没有凭证或凭证已使用都会拒绝 =====
        cancellationAuthorization.consume(runId, orderId);
        // ===== 3) SQL 再次限定可取消状态，解决校验与更新之间的并发竞争 =====
        int updated = repository.cancel(orderId, reason);
        if (updated != 1) {
            throw new IllegalStateException("订单状态已变化，取消失败");
        }

        // ===== 4) 状态更新后删除旧缓存，再查询数据库返回最新订单 =====
        redis.delete("cache:order:" + orderId);
        return getOrder(orderId);
    }
}
