package com.cloude.shop.service.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.enums.OrderStatus;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时订单兜底关单任务
 * <p>
 * 兜底场景：MQ 延迟消息丢失 / 消费失败时，延迟关单不会触发。
 * 本任务每分钟扫描超时（> TTL + 60s 缓冲）仍待付款的订单并关闭归还库存。
 * closeOrder 内部为 CAS 状态流转，与 MQ 消费者并发执行时天然幂等，双跑无害。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final long BUFFER_SECONDS = 60;
    private static final int BATCH_SIZE = 200;

    /** 订单支付超时时间（毫秒），须与 shop.order.delay-ttl-ms 保持一致 */
    @Value("${shop.order.delay-ttl-ms:1800000}")
    private long orderTtlMs;

    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusSeconds(orderTtlMs / 1000 + BUFFER_SECONDS);
        List<OmsOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .select(OmsOrder::getId, OmsOrder::getOrderNo)
                .eq(OmsOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .lt(OmsOrder::getCreateTime, cutoff)
                .last("limit " + BATCH_SIZE));
        if (orders.isEmpty()) {
            return;
        }
        log.info("兜底关单任务: 发现 {} 笔超时未支付订单", orders.size());
        for (OmsOrder order : orders) {
            try {
                orderService.closeOrder(order.getOrderNo(), "超时未支付，系统自动关闭");
            } catch (Exception e) {
                log.error("兜底关单失败, orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
