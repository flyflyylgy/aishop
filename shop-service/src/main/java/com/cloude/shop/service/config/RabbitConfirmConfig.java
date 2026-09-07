package com.cloude.shop.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 投递可靠性
 * <p>
 * - returns 回调：捕获不可路由消息（配置 mandatory=true + publisher-returns=true）
 * - confirm 确认：发送方使用 CorrelationData 逐条确认（见 OrderService），失败记录错误日志
 * - 兜底：即使消息丢失，OrderTimeoutJob 会扫描超时订单自动关单
 */
@Slf4j
@Configuration
public class RabbitConfirmConfig {

    @Bean
    public ApplicationRunner rabbitReturnsConfigurer(RabbitTemplate rabbitTemplate) {
        return args -> {
            rabbitTemplate.setReturnsCallback(returned ->
                    log.error("RabbitMQ 消息不可路由, exchange={}, routingKey={}, replyText={}",
                            returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
            log.info("RabbitMQ returns 回调已启用（confirm 通过 CorrelationData 逐条确认）");
        };
    }
}
