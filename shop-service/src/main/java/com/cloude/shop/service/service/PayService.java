package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.common.enums.OrderStatus;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.OmsOrderItem;
import com.cloude.shop.mapper.entity.OmsPayLog;
import com.cloude.shop.mapper.mapper.OmsOrderItemMapper;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.OmsPayLogMapper;
import com.cloude.shop.service.dto.PayNotifyParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付服务 —— 支付幂等（三态 CAS + 回调日志唯一约束）
 * <p>
 * 幂等设计（参考 EasyMall）：
 * 1. oms_pay_log.pay_no 唯一约束：同一流水号回调只处理一次
 * 2. 订单状态 CAS（0待付款 -> 1已付款）：并发回调只有一笔能成功
 * 3. 金额核对：回调金额必须等于订单应付金额
 * <p>
 * 注：生产环境对接微信/支付宝 SDK，此处为模拟支付通道，回调接口由联调方调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private static final DateTimeFormatter PAY_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final OmsPayLogMapper payLogMapper;
    private final StockService stockService;

    /**
     * 创建支付单（模拟）：生成支付流水号，实际项目中此处调用微信/支付宝统一下单
     */
    public Map<String, Object> createPay(Long memberId, String orderNo) {
        OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getOrderNo, orderNo));
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        if (OrderStatus.of(order.getStatus()) != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }
        String payNo = "PAY" + LocalDateTime.now().format(PAY_NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        return Map.of(
                "payNo", payNo,
                "orderNo", orderNo,
                "amount", order.getPayAmount(),
                "qrContent", "simulated-pay://order/" + orderNo + "/" + payNo
        );
    }

    /**
     * 处理支付回调（幂等 + 对账核对）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleNotify(PayNotifyParam param) {
        // 1. 幂等：同一 pay_no 已处理过则直接返回
        OmsPayLog existing = payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPayNo, param.getPayNo()));
        if (existing != null) {
            log.info("支付回调重复, 忽略. payNo={}, orderNo={}", param.getPayNo(), param.getOrderNo());
            return;
        }

        OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getOrderNo, param.getOrderNo()));
        if (order == null) {
            throw new BusinessException("回调订单不存在: " + param.getOrderNo());
        }

        // 2. 记录回调日志（审计 + 唯一约束兜底幂等）
        OmsPayLog payLog = new OmsPayLog();
        payLog.setPayNo(param.getPayNo());
        payLog.setOrderNo(param.getOrderNo());
        payLog.setAmount(param.getAmount());
        payLog.setSuccess(Boolean.TRUE.equals(param.getSuccess()) ? 1 : 0);
        payLog.setCallbackBody(param.getBody());
        payLogMapper.insert(payLog);

        // 3. 失败回调：保持待付款，等待超时关单或重试
        if (!Boolean.TRUE.equals(param.getSuccess())) {
            throw new BusinessException(ResultCode.PAY_FAILED);
        }

        // 4. 金额核对
        if (order.getPayAmount().compareTo(param.getAmount()) != 0) {
            throw new BusinessException("回调金额与订单应付金额不一致");
        }

        // 5. 状态 CAS：待付款 -> 已付款（并发回调只有一笔成功）
        int affected = orderMapper.casMarkPaid(param.getOrderNo(), 0, LocalDateTime.now());
        if (affected == 0) {
            OrderStatus current = OrderStatus.of(
                    orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                            .eq(OmsOrder::getOrderNo, param.getOrderNo())).getStatus());
            if (current == OrderStatus.PAID) {
                log.info("订单已支付, 回调幂等返回. orderNo={}", param.getOrderNo());
                return;
            }
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }

        // 6. 确认库存：locked -> sold
        List<OmsOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderNo, param.getOrderNo()));
        for (OmsOrderItem item : items) {
            if (item.getSkuId() != null) {
                stockService.confirmSkuSold(item.getSkuId(), item.getQuantity());
            } else {
                stockService.confirmSold(item.getProductId(), item.getQuantity());
            }
        }
        log.info("支付成功, 订单已确认. orderNo={}, payNo={}", param.getOrderNo(), param.getPayNo());
    }
}
