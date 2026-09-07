package com.cloude.shop.common.config;

import com.cloude.shop.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * JWT Bean 装配 + 启动校验
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties properties;

    @Bean
    public JwtUtil jwtUtil() {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("shop.jwt.secret 未配置或长度不足 32 字节，拒绝启动");
        }
        if (secret.startsWith("dev-only")) {
            log.warn("==============================================================");
            log.warn("当前使用开发默认 JWT 密钥，生产环境必须通过环境变量 JWT_SECRET 注入随机密钥！");
            log.warn("==============================================================");
        }
        return new JwtUtil(secret, properties.getExpireMinutes() * 60 * 1000L);
    }
}
