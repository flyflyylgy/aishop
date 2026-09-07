package com.cloude.shop.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "shop.jwt")
public class JwtProperties {

    /** 签名密钥（生产环境必须通过 JWT_SECRET 环境变量注入随机密钥，长度 >= 32 字节） */
    private String secret;

    /** token 有效期（分钟），默认 24 小时 */
    private long expireMinutes = 1440;
}
