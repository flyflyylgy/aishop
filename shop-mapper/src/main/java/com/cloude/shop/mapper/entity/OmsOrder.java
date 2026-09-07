package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 * 状态机：0待付款 -> 1已付款 -> 2已发货 -> 3已完成；0/1 -> 4已关闭
 */
@Data
@TableName("oms_order")
public class OmsOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long memberId;

    /** 0-待付款 1-已付款 2-已发货 3-已完成 4-已关闭 */
    private Integer status;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddr;

    private String note;

    /** 0-模拟支付 */
    private Integer payType;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private String expressCompany;

    private String expressNo;

    private LocalDateTime finishTime;

    private LocalDateTime closeTime;

    private String closeReason;

    private String adminRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
