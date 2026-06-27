package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.config.HarnessProperties;
import com.aftersales.platform.agent.domain.Enums.OrderStatus;
import com.aftersales.platform.agent.domain.OrderInfo;
import com.aftersales.platform.agent.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCancellationGuardTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JsonService json = mock(JsonService.class);
    private final CancellationAuthorizationService authorization = mock(CancellationAuthorizationService.class);
    private final HarnessProperties properties = new HarnessProperties();
    private OrderService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        properties.setRedisCacheSeconds(60);
        service = new OrderService(repository, properties, redis, json, authorization);
    }

    @Test
    void cancelledOrderIsIdempotentAndDoesNotConsumeAuthorization() {
        OrderInfo cancelled = order(OrderStatus.CANCELLED);
        when(repository.findById(10001L)).thenReturn(Optional.of(cancelled));

        assertThat(service.cancelOrder(9L, 10001L, "重复请求")).isEqualTo(cancelled);
        verifyNoInteractions(authorization);
        verify(repository, never()).cancel(10001L, "重复请求");
    }

    @Test
    void paidOrderCannotBeCancelledWithoutApprovalAuthorization() {
        when(repository.findById(10001L)).thenReturn(Optional.of(order(OrderStatus.PAID)));
        org.mockito.Mockito.doThrow(new IllegalStateException("取消订单缺少有效审批授权"))
                .when(authorization).consume(9L, 10001L);

        assertThatThrownBy(() -> service.cancelOrder(9L, 10001L, "绕过审批"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少有效审批授权");
        verify(repository, never()).cancel(10001L, "绕过审批");
    }

    @Test
    void approvedPaidOrderCanBeCancelledExactlyOnce() {
        OrderInfo paid = order(OrderStatus.PAID);
        OrderInfo cancelled = order(OrderStatus.CANCELLED);
        when(repository.findById(10001L))
                .thenReturn(Optional.of(paid))
                .thenReturn(Optional.of(cancelled));
        when(repository.cancel(10001L, "用户不想要了")).thenReturn(1);

        assertThat(service.cancelOrder(9L, 10001L, "用户不想要了").status())
                .isEqualTo(OrderStatus.CANCELLED);
        verify(authorization).consume(9L, 10001L);
        verify(repository).cancel(10001L, "用户不想要了");
    }

    private OrderInfo order(OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new OrderInfo(10001L, 10086L, 1L, status, now, now, null, null);
    }
}
