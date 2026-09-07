package com.cloude.shop.common.component;

import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 登录拦截器：
 * 1. 从 Authorization Header 提取 Bearer token
 * 2. 校验 JWT 签名/有效期
 * 3. 校验 Redis 登录态（支持服务端吊销）
 * 4. 填充 UserContext
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(HEADER);
        if (!StringUtils.hasText(token) || !token.startsWith(PREFIX)) {
            return reject(response);
        }
        token = token.substring(PREFIX.length());
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            return reject(response);
        }
        String role = claims.get("role", String.class);
        String tokenKey = ("admin".equals(role) ? RedisKeyConstant.ADMIN_TOKEN : RedisKeyConstant.MEMBER_TOKEN) + token;
        Boolean exists = stringRedisTemplate.hasKey(tokenKey);
        if (Boolean.FALSE.equals(exists)) {
            return reject(response);
        }
        // 刷新登录态过期时间（滑动过期）
        stringRedisTemplate.expire(tokenKey, RedisKeyConstant.TOKEN_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        UserContext.set(Long.valueOf(claims.getSubject()), role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        byte[] body = objectMapper.writeValueAsBytes(CommonResult.unauthorized(ResultCode.UNAUTHORIZED.getMessage()));
        response.getOutputStream().write(body);
        response.getOutputStream().flush();
        return false;
    }
}
