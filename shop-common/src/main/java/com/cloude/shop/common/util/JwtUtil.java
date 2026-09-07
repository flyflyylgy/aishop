package com.cloude.shop.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（登录态签发与校验）
 * 密钥必须通过 JwtConfig 从配置注入，禁止硬编码默认密钥
 */
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(String secret, long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 token：subject = 用户ID，claims.role 区分会员/管理员
     */
    public String generate(Long userId, String role, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token，非法/过期返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 中提取用户 ID（失败返回 null）
     */
    public Long getUserId(String token) {
        Claims claims = parse(token);
        return claims == null ? null : Long.valueOf(claims.getSubject());
    }
}
