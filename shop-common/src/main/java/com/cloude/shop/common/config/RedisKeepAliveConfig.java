package com.cloude.shop.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis 连接保活配置。
 *
 * 背景：Docker/WSL2 空闲一段时间后会静默回收 TCP 连接，客户端连接池中的
 * 半开连接在写入时才失败（Unable to write command into connection）。
 *
 * 防护手段：
 * - SocketOptions.keepAlive(true) + keepAliveIdleTime(60s) + keepAliveInterval(15s)：
 *   连接空闲 60 秒后由操作系统每 15 秒发送 TCP 探测包，
 *   死连接在内核层被立即标记断开（核心根治手段，Lettuce 6.2+ 支持）
 * - autoReconnect(true)：TCP 断开后 ConnectionWatchdog 自动重建连接
 * - pingBeforeActivateConnection(true)：连接激活前先 PING 校验
 *
 * 另配合各应用 yml 中 spring.data.redis.lettuce.pool.time-between-eviction-runs=30s
 * 定期清理长期空闲的池化对象。
 */
@Configuration
public class RedisKeepAliveConfig {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceKeepAliveCustomizer() {
        return builder -> builder.clientOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .socketOptions(SocketOptions.builder()
                        // TCP keepalive：空闲 60s 后内核每 15s 发探测包，死连接立即被内核标记断开
                        .keepAlive(SocketOptions.KeepAliveOptions.builder()
                                .idle(Duration.ofSeconds(60))
                                .interval(Duration.ofSeconds(15))
                                .build())
                        .connectTimeout(Duration.ofSeconds(10))
                        .build())
                .build());
    }
}
