package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后退款单
 * 状态：0-待审核 1-已同意 2-已拒绝
 */
@Data
@TableName("oms_refund")
public class OmsRefund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String orderNo;
    private Long memberId;
    private String reason;
    private BigDecimal amount;
    private Integer status;
    private String adminRemark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
