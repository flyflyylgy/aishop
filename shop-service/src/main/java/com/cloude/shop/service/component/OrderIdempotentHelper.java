package com.cloude.shop.service.component;

import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 下单幂等助手：基于 Redis SETNX 的防重复提交
 * <p>
 * 流程：tryAcquire 占位 -> 业务执行 -> complete 写回订单号 / release 释放允许重试
 * 同一 (memberId, idempotentKey) 10 分钟内只允许一次成功下单
 */
@Component
@RequiredArgsConstructor
public class OrderIdempotentHelper {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);
    private static final String PROCESSING = "processing";

    private final StringRedisTemplate stringRedisTemplate;

    private String key(Long memberId, String idempotentKey) {
        return RedisKeyConstant.ORDER_IDEM_PREFIX + memberId + ":" + idempotentKey;
    }

    /**
     * 占位：重复请求直接拒绝（首单成功后重复请求提示已提交）
     */
    public void tryAcquire(Long memberId, String idempotentKey) {
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(key(memberId, idempotentKey), PROCESSING, HOLD_DURATION);
        if (Boolean.FALSE.equals(first)) {
            String value = stringRedisTemplate.opsForValue().get(key(memberId, idempotentKey));
            if (value != null && !PROCESSING.equals(value)) {
                throw new BusinessException("订单已提交（" + value + "），请勿重复下单");
            }
            throw new BusinessException("订单正在处理中，请勿重复提交");
        }
    }

    /**
     * 成功后写回订单号，重复请求可获知订单号
     */
    public void complete(Long memberId, String idempotentKey, String orderNo) {
        stringRedisTemplate.opsForValue().set(key(memberId, idempotentKey), orderNo, HOLD_DURATION);
    }

    /**
     * 业务失败释放占位，允许客户端重试
     */
    public void release(Long memberId, String idempotentKey) {
        stringRedisTemplate.delete(key(memberId, idempotentKey));
    }
}
