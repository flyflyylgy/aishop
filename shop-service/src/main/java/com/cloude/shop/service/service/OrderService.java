package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.constant.MqConstant;
import com.cloude.shop.common.enums.OrderStatus;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.OmsOrderItem;
import com.cloude.shop.mapper.entity.OmsCartItem;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsSku;
import com.cloude.shop.mapper.entity.UmsMemberAddress;
import com.cloude.shop.mapper.mapper.OmsCartItemMapper;
import com.cloude.shop.mapper.mapper.OmsOrderItemMapper;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.mapper.mapper.PmsSkuMapper;
import com.cloude.shop.service.dto.OrderCreateParam;
import com.cloude.shop.service.dto.OrderDetailVO;
import com.cloude.shop.service.dto.OrderItemParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务 —— 订单状态机 + 库存事务 + MQ 延迟关单
 * <p>
 * 核心设计（参考 mall / EasyMall）：
 * 1. 下单：CAS 锁库存（available -> locked）+ 订单落库 + 清购物车，同一事务
 * 2. 延迟关单：事务提交后发 TTL 消息，30 分钟未支付自动关单归还库存
 * 3. 状态流转：全部通过 OmsOrderMapper 的 CAS SQL（前置状态条件），天然防止并发竞争
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final PmsProductMapper productMapper;
    private final PmsSkuMapper skuMapper;
    private final OmsCartItemMapper cartItemMapper;
    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;
    private final MemberAddressService memberAddressService;
    private final CouponService couponService;

    /**
     * 创建订单（下单）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(Long memberId, OrderCreateParam param) {
        // 1. 逐项校验商品并锁库存（CAS，不足即抛异常回滚）
        if (param.getItems() == null || param.getItems().isEmpty()) {
            throw new BusinessException("订单商品列表不能为空");
        }
        // 收货人信息：优先使用地址簿，否则校验手填字段
        UmsMemberAddress address = null;
        if (param.getAddressId() != null) {
            address = memberAddressService.getOwned(memberId, param.getAddressId());
        } else if (!StringUtils.hasText(param.getReceiverName())
                || !StringUtils.hasText(param.getReceiverPhone())
                || !StringUtils.hasText(param.getReceiverAddr())) {
            throw new BusinessException("请选择收货地址或填写完整收货人信息");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, PmsProduct> productMap = productMapper.selectBatchIds(
                        param.getItems().stream().map(i -> i.getProductId()).toList()).stream()
                .collect(Collectors.toMap(PmsProduct::getId, Function.identity()));

        // 预取 SKU（has_sku=1 的商品）
        List<Long> skuIds = param.getItems().stream()
                .map(OrderItemParam::getSkuId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, PmsSku> skuMap = skuIds.isEmpty() ? Map.of()
                : skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(PmsSku::getId, Function.identity()));

        for (OrderItemParam itemParam : param.getItems()) {
            PmsProduct product = productMap.get(itemParam.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BusinessException("商品不存在或已下架(ID:" + itemParam.getProductId() + ")");
            }
            // 多规格商品走 SKU 库存/价格；单规格走 product 库存/价格
            if (Integer.valueOf(1).equals(product.getHasSku()) && itemParam.getSkuId() != null) {
                PmsSku sku = skuMap.get(itemParam.getSkuId());
                if (sku == null || !sku.getProductId().equals(product.getId()) || sku.getStatus() != 1) {
                    throw new BusinessException("SKU 不存在或已禁用");
                }
                stockService.lockSkuStock(sku.getId(), itemParam.getQuantity());
                BigDecimal subTotal = sku.getPrice().multiply(BigDecimal.valueOf(itemParam.getQuantity()));
                totalAmount = totalAmount.add(subTotal);
            } else {
                stockService.lockStock(product.getId(), itemParam.getQuantity());
                BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(itemParam.getQuantity()));
                totalAmount = totalAmount.add(subTotal);
            }
        }

        // 2. 优惠券校验 + 折扣计算（可选）
        CouponService.CouponUse couponUse = null;
        if (param.getCouponHistoryId() != null) {
            couponUse = couponService.validateAndCalc(memberId, param.getCouponHistoryId(), totalAmount);
        }
        BigDecimal payAmount = couponUse != null
                ? totalAmount.subtract(couponUse.getDiscount())
                : totalAmount;

        // 3. 订单落库
        String orderNo = generateOrderNo(memberId);
        OmsOrder order = new OmsOrder();
        order.setOrderNo(orderNo);
        order.setMemberId(memberId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        if (couponUse != null) {
            order.setCouponId(couponUse.getCouponId());
            order.setCouponName(couponUse.getCouponName());
            order.setCouponAmount(couponUse.getDiscount());
        }
        order.setReceiverName(address != null ? address.getReceiverName() : param.getReceiverName());
        order.setReceiverPhone(address != null ? address.getReceiverPhone() : param.getReceiverPhone());
        order.setReceiverAddr(address != null ? address.getReceiverAddr() : param.getReceiverAddr());
        order.setNote(param.getNote());
        orderMapper.insert(order);

        // 核销优惠券（CAS 0->1，失败抛错回滚整个订单事务）
        if (couponUse != null) {
            couponService.markUsed(couponUse.getHistoryId(), orderNo);
        }

        // 3. 订单明细快照
        for (OrderItemParam itemParam : param.getItems()) {
            PmsProduct product = productMap.get(itemParam.getProductId());
            PmsSku sku = (Integer.valueOf(1).equals(product.getHasSku()) && itemParam.getSkuId() != null)
                    ? skuMap.get(itemParam.getSkuId()) : null;
            BigDecimal itemPrice = sku != null ? sku.getPrice() : product.getPrice();
            OmsOrderItem item = new OmsOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setProductId(product.getId());
            item.setSkuId(sku != null ? sku.getId() : null);
            item.setSpecValues(sku != null ? sku.getSpecValues() : null);
            item.setProductPic(product.getMainImage());
            item.setProductName(product.getName());
            item.setPrice(itemPrice);
            item.setQuantity(itemParam.getQuantity());
            item.setTotalPrice(itemPrice.multiply(BigDecimal.valueOf(itemParam.getQuantity())));
            orderItemMapper.insert(item);
        }

        // 4. 清除购物车中已下单商品（按 productId，旧单规格条目 skuId=null 也会被清掉）
        cartItemMapper.delete(new LambdaQueryWrapper<OmsCartItem>()
                .eq(OmsCartItem::getMemberId, memberId)
                .in(OmsCartItem::getProductId, param.getItems().stream()
                        .map(OrderItemParam::getProductId).toList()));

        // 5. 事务提交后发送延迟关单消息（防止消息先于数据落库）
        sendDelayCloseMessageAfterCommit(orderNo);

        return buildDetail(order, orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderNo, orderNo)));
    }

    private void sendDelayCloseMessageAfterCommit(String orderNo) {
        Runnable send = () -> {
            // CorrelationData 逐条 confirm：投递失败记录错误日志，由 OrderTimeoutJob 兜底关单
            CorrelationData correlationData = new CorrelationData(orderNo);
            rabbitTemplate.convertAndSend(MqConstant.DELAY_EXCHANGE, MqConstant.DELAY_ROUTING_KEY, orderNo, correlationData);
            correlationData.getFuture().whenComplete((confirm, ex) -> {
                if (ex != null || confirm == null || !confirm.isAck()) {
                    String cause = ex != null ? ex.getMessage()
                            : (confirm == null ? "no-confirm" : confirm.getReason());
                    log.error("延迟关单消息投递失败(将由兜底任务关单), orderNo={}, cause={}", orderNo, cause);
                } else {
                    log.info("延迟关单消息已投递, orderNo={}", orderNo);
                }
            });
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    /**
     * 关单（超时关单/用户取消）：
     * CAS 待付款 -> 已关闭，成功后释放锁定库存
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(String orderNo, String reason) {
        int affected = orderMapper.casClosePendingOrder(orderNo, LocalDateTime.now(), reason);
        if (affected == 0) {
            // 并发竞争：订单已支付或已关闭，幂等返回
            log.info("关单跳过(状态已变更), orderNo={}", orderNo);
            return false;
        }
        List<OmsOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderNo, orderNo));
        for (OmsOrderItem item : items) {
            if (item.getSkuId() != null) {
                stockService.releaseSkuStock(item.getSkuId(), item.getQuantity());
            } else {
                stockService.releaseStock(item.getProductId(), item.getQuantity());
            }
        }
        // 回退优惠券（恢复未使用；已过期则置为过期）
        couponService.releaseByOrder(orderNo);
        log.info("订单已关闭并释放库存, orderNo={}", orderNo);
        return true;
    }

    /**
     * 会员取消订单（仅本人待付款订单）
     */
    public void cancelOrder(Long memberId, String orderNo) {
        OmsOrder order = getOwnedOrder(memberId, orderNo);
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (!current.canTransferTo(OrderStatus.CLOSED)) {
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }
        closeOrder(orderNo, "用户取消");
    }

    /**
     * 确认收货：CAS 已发货 -> 已完成
     */
    public void confirmReceive(Long memberId, Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (!current.canTransferTo(OrderStatus.COMPLETED)) {
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }
        orderMapper.casComplete(orderId, LocalDateTime.now());
    }

    /**
     * 后台发货：CAS 已付款 -> 已发货（登记物流公司与运单号）
     */
    public void ship(Long orderId, String expressCompany, String expressNo) {
        if (expressCompany == null || expressCompany.isBlank()
                || expressNo == null || expressNo.isBlank()) {
            throw new BusinessException("物流公司和运单号不能为空");
        }
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (!current.canTransferTo(OrderStatus.SHIPPED)) {
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }
        int affected = orderMapper.casShip(orderId, LocalDateTime.now(), expressCompany.trim(), expressNo.trim());
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATE_ILLEGAL);
        }
    }

    /**
     * 后台订单备注（仅后台可见）
     */
    public void updateAdminRemark(Long orderId, String remark) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setAdminRemark(remark);
        orderMapper.updateById(update);
    }

    /**
     * 后台订单详情
     */
    public OrderDetailVO adminDetail(Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        List<OmsOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderId, orderId));
        return buildDetail(order, items);
    }

    /**
     * 会员订单分页
     */
    public Page<OrderDetailVO> page(Long memberId, Integer status, Integer pageNum, Integer pageSize) {
        Page<OmsOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(OmsOrder::getMemberId, memberId)
                        .eq(status != null, OmsOrder::getStatus, status)
                        .orderByDesc(OmsOrder::getId));
        return toDetailPage(page);
    }

    /**
     * 后台订单分页（支持状态 + 订单号/收货人模糊筛选）
     */
    public Page<OrderDetailVO> adminPage(Integer status, String keyword, Integer pageNum, Integer pageSize) {
        Page<OmsOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OmsOrder>()
                        .eq(status != null, OmsOrder::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(OmsOrder::getOrderNo, keyword.trim())
                                .or().like(OmsOrder::getReceiverName, keyword.trim())
                                .or().like(OmsOrder::getReceiverPhone, keyword.trim()))
                        .orderByDesc(OmsOrder::getId));
        return toDetailPage(page);
    }

    public OrderDetailVO detail(Long memberId, String orderNo) {
        OmsOrder order = getOwnedOrder(memberId, orderNo);
        List<OmsOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderNo, orderNo));
        return buildDetail(order, items);
    }

    private OmsOrder getOwnedOrder(Long memberId, String orderNo) {
        OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getOrderNo, orderNo));
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Page<OrderDetailVO> toDetailPage(Page<OmsOrder> page) {
        List<Long> orderIds = page.getRecords().stream().map(OmsOrder::getId).toList();
        Map<Long, List<OmsOrderItem>> itemMap = orderIds.isEmpty() ? Map.of()
                : orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                        .in(OmsOrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OmsOrderItem::getOrderId));
        Page<OrderDetailVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(o -> buildDetail(o, itemMap.getOrDefault(o.getId(), List.of())))
                .collect(Collectors.toList()));
        return result;
    }

    private OrderDetailVO buildDetail(OmsOrder order, List<OmsOrderItem> items) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMemberId(order.getMemberId());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setCouponId(order.getCouponId());
        vo.setCouponName(order.getCouponName());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddr(order.getReceiverAddr());
        vo.setNote(order.getNote());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setExpressCompany(order.getExpressCompany());
        vo.setExpressNo(order.getExpressNo());
        vo.setFinishTime(order.getFinishTime());
        vo.setCloseTime(order.getCloseTime());
        vo.setCloseReason(order.getCloseReason());
        vo.setAdminRemark(order.getAdminRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        vo.setStatusDesc(OrderStatus.of(order.getStatus()).getDesc());
        return vo;
    }

    private String generateOrderNo(Long memberId) {
        return LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000))
                + String.format("%04d", memberId % 10000);
    }
}
