package com.cloude.shop.service.component;

import com.cloude.shop.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 下单幂等助手行为测试（Mockito 模拟 Redis，无需中间件）
 */
@ExtendWith(MockitoExtension.class)
class OrderIdempotentHelperTest {

    private static final String KEY = "shop:idem:order:1:idem-001";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OrderIdempotentHelper helper;

    @BeforeEach
    void setUp() {
        helper = new OrderIdempotentHelper(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acquireSucceedsOnFirstRequest() {
        when(valueOperations.setIfAbsent(eq(KEY), eq("processing"), any(Duration.class))).thenReturn(true);
        assertDoesNotThrow(() -> helper.tryAcquire(1L, "idem-001"));
    }

    @Test
    void acquireRejectsWhenOrderCommitted() {
        when(valueOperations.setIfAbsent(eq(KEY), eq("processing"), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(KEY)).thenReturn("202608291200000000010001");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> helper.tryAcquire(1L, "idem-001"));
        assertTrue(ex.getMessage().contains("订单已提交"));
    }

    @Test
    void acquireRejectsWhenProcessing() {
        when(valueOperations.setIfAbsent(eq(KEY), eq("processing"), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(KEY)).thenReturn("processing");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> helper.tryAcquire(1L, "idem-001"));
        assertTrue(ex.getMessage().contains("处理中"));
    }

    @Test
    void completeWritesOrderNo() {
        helper.complete(1L, "idem-001", "ORDER-001");
        verify(valueOperations).set(eq(KEY), eq("ORDER-001"), any(Duration.class));
    }

    @Test
    void releaseDeletesKey() {
        helper.release(1L, "idem-001");
        verify(stringRedisTemplate).delete(KEY);
    }

    @Test
    void releaseNeverCalledOnSuccess() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        helper.tryAcquire(1L, "idem-001");
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
