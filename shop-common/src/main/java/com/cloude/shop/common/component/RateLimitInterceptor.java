package com.cloude.shop.common.component;

import com.cloude.shop.common.api.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Redis 固定窗口限流拦截器
 * - 敏感接口（login/register/notify）：每 IP 60 秒 10 次
 * - 其余接口：每 IP 10 秒 100 次
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String[] SENSITIVE_SUFFIX = {"/login", "/register", "/notify"};
    private static final int SENSITIVE_LIMIT = 10;
    private static final long SENSITIVE_WINDOW_MS = 60_000;
    private static final int GLOBAL_LIMIT = 100;
    private static final long GLOBAL_WINDOW_MS = 10_000;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        boolean sensitive = false;
        for (String suffix : SENSITIVE_SUFFIX) {
            if (uri.endsWith(suffix)) {
                sensitive = true;
                break;
            }
        }
        int limit = sensitive ? SENSITIVE_LIMIT : GLOBAL_LIMIT;
        long windowMs = sensitive ? SENSITIVE_WINDOW_MS : GLOBAL_WINDOW_MS;
        String bucket = sensitive ? "sens" : "g";

        long windowIndex = System.currentTimeMillis() / windowMs;
        String key = com.cloude.shop.common.constant.RedisKeyConstant.RATE_LIMIT_PREFIX
                + bucket + ":" + clientIp(request) + ":" + windowIndex;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMillis(windowMs * 2));
        }
        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getOutputStream().write(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsBytes(CommonResult.failed(429, "请求过于频繁，请稍后重试")));
            response.getOutputStream().flush();
            return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
