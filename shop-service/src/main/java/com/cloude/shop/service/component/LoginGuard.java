package com.cloude.shop.service.component;

import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 账号级登录失败锁定：
 * 同一账号连续失败 5 次锁定 15 分钟（区别于 IP 限流，防单账号暴力破解）
 */
@Component
@RequiredArgsConstructor
public class LoginGuard {

    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    private String key(String role, String username) {
        return RedisKeyConstant.LOGIN_FAIL_PREFIX + role + ":" + username;
    }

    /**
     * 登录前校验：是否已被锁定
     */
    public void assertNotLocked(String role, String username) {
        String count = stringRedisTemplate.opsForValue().get(key(role, username));
        if (count != null && Long.parseLong(count) >= MAX_FAIL) {
            throw new BusinessException("账号已锁定，请 15 分钟后再试");
        }
    }

    /**
     * 记录一次失败（首次失败起 15 分钟计数窗口）
     */
    public void recordFailure(String role, String username) {
        String k = key(role, username);
        Long count = stringRedisTemplate.opsForValue().increment(k);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(k, LOCK_WINDOW);
        }
        if (count != null && count == MAX_FAIL) {
            stringRedisTemplate.expire(k, LOCK_WINDOW);
        }
    }

    /**
     * 登录成功清除计数
     */
    public void clearFailures(String role, String username) {
        stringRedisTemplate.delete(key(role, username));
    }
}
