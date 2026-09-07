package com.cloude.shop.portal.mq;

import com.cloude.shop.common.constant.MqConstant;
import com.cloude.shop.service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 延迟关单消费者：监听死信队列，超时未支付订单自动关闭并释放库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseListener {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstant.CLOSE_QUEUE)
    public void onMessage(String orderNo) {
        log.info("收到延迟关单消息, orderNo={}", orderNo);
        try {
            orderService.closeOrder(orderNo, "超时未支付，系统自动关闭");
        } catch (Exception e) {
            log.error("关单处理失败, orderNo={}", orderNo, e);
            throw e;
        }
    }
}
