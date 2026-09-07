package com.cloude.shop.common.constant;

/**
 * RabbitMQ 常量：TTL + DLX 延迟关单拓扑
 */
public final class MqConstant {

    private MqConstant() {
    }

    /** 延迟队列交换机：消息先进入延迟队列等待 TTL 过期 */
    public static final String DELAY_EXCHANGE = "shop.order.delay.exchange";
    /** 延迟队列（x-message-ttl = 30min，到期死信转发到关单交换机） */
    public static final String DELAY_QUEUE = "shop.order.delay.queue";
    /** 延迟队列路由键 */
    public static final String DELAY_ROUTING_KEY = "order.close.delay";

    /** 关单交换机：死信落地 */
    public static final String CLOSE_EXCHANGE = "shop.order.close.exchange";
    /** 关单队列：消费者监听 */
    public static final String CLOSE_QUEUE = "shop.order.close.queue";
    /** 关单路由键 */
    public static final String CLOSE_ROUTING_KEY = "order.close";
}
