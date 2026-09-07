package com.cloude.shop.service.config;

import com.cloude.shop.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑：TTL + DLX 实现延迟关单
 * 订单消息 -> 延迟队列(默认30min TTL) -> 死信转发 -> 关单队列 -> 消费者关单
 */
@Configuration
public class RabbitConfig {

    /** 订单支付超时时间（毫秒），默认 30 分钟 */
    @Value("${shop.order.delay-ttl-ms:1800000}")
    private long orderTtlMs;

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(MqConstant.DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", orderTtlMs);
        args.put("x-dead-letter-exchange", MqConstant.CLOSE_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.CLOSE_ROUTING_KEY);
        return new Queue(MqConstant.DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConstant.DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange closeExchange() {
        return new DirectExchange(MqConstant.CLOSE_EXCHANGE, true, false);
    }

    @Bean
    public Queue closeQueue() {
        return new Queue(MqConstant.CLOSE_QUEUE, true);
    }

    @Bean
    public Binding closeBinding() {
        return BindingBuilder.bind(closeQueue()).to(closeExchange()).with(MqConstant.CLOSE_ROUTING_KEY);
    }
}
