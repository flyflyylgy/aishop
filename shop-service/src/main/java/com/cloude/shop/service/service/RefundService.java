package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.OmsRefund;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.OmsRefundMapper;
import com.cloude.shop.service.dto.RefundApplyParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后退款服务
 * 状态：0-待审核 1-已同意 2-已拒绝
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final OmsRefundMapper refundMapper;
    private final OmsOrderMapper orderMapper;
    private final MessageService messageService;

    /**
     * 会员申请退款（已付款/已发货订单可申请，已完成订单 7 天内可申请）
     */
    @Transactional(rollbackFor = Exception.class)
    public void apply(RefundApplyParam param) {
        Long memberId = UserContext.getUserId();
        OmsOrder order = orderMapper.selectById(param.getOrderId());
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        int status = order.getStatus();
        // 1-已付款、2-已发货、3-已完成（7天内）可申请
        if (status != 1 && status != 2 && status != 3) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        if (status == 3 && order.getFinishTime() != null) {
            long days = java.time.Duration.between(order.getFinishTime(), LocalDateTime.now()).toDays();
            if (days > 7) {
                throw new BusinessException("已完成订单超过 7 天不可退款");
            }
        }
        // 防重复申请（存在待审核/已同意的退款单）
        long exist = refundMapper.selectCount(new LambdaQueryWrapper<OmsRefund>()
                .eq(OmsRefund::getOrderId, param.getOrderId())
                .in(OmsRefund::getStatus, List.of(0, 1)));
        if (exist > 0) {
            throw new BusinessException("该订单已有退款申请在处理中");
        }

        OmsRefund refund = new OmsRefund();
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setMemberId(memberId);
        refund.setReason(param.getReason());
        refund.setAmount(order.getPayAmount());
        refund.setStatus(0);
        refundMapper.insert(refund);
    }

    /**
     * 我的退款列表
     */
    public Page<OmsRefund> myRefunds(Integer pageNum, Integer pageSize) {
        return refundMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OmsRefund>()
                        .eq(OmsRefund::getMemberId, UserContext.getUserId())
                        .orderByDesc(OmsRefund::getId));
    }

    /**
     * 后台退款分页
     */
    public Page<OmsRefund> adminPage(Integer status, Integer pageNum, Integer pageSize) {
        return refundMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OmsRefund>()
                        .eq(status != null, OmsRefund::getStatus, status)
                        .orderByDesc(OmsRefund::getId));
    }

    /**
     * 后台审批退款
     * @param refundId 退款单ID
     * @param approved true=同意 false=拒绝
     * @param adminRemark 管理员备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long refundId, boolean approved, String adminRemark) {
        OmsRefund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException("退款单已处理");
        }
        OmsRefund update = new OmsRefund();
        update.setId(refundId);
        update.setStatus(approved ? 1 : 2);
        update.setAdminRemark(adminRemark);
        update.setHandleTime(LocalDateTime.now());
        refundMapper.updateById(update);

        // 同意退款 → 订单关闭
        if (approved) {
            OmsOrder order = orderMapper.selectById(refund.getOrderId());
            if (order != null && order.getStatus() != 4) {
                OmsOrder o = new OmsOrder();
                o.setId(order.getId());
                o.setStatus(4);
                o.setCloseReason("退款成功: " + refund.getReason());
                o.setCloseTime(LocalDateTime.now());
                orderMapper.updateById(o);
            }
        }
        // 站内信：退款审批结果通知
        String title = approved ? "退款已通过" : "退款申请未通过";
        String content = "您对订单 " + refund.getOrderNo() + " 的退款申请"
                + (approved ? "已通过，退款金额 ¥" + refund.getAmount() + " 将原路退回。"
                : "未通过审核。" + (adminRemark == null ? "" : "备注：" + adminRemark));
        messageService.send(refund.getMemberId(), MessageService.TYPE_AFTER_SALE, title, content,
                "refund", refund.getOrderNo());
    }
}
