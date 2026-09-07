package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调日志（幂等核对 + 审计）
 */
@Data
@TableName("oms_pay_log")
public class OmsPayLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 第三方支付流水号 */
    private String payNo;

    private String orderNo;

    private BigDecimal amount;

    /** 0-失败 1-成功 */
    private Integer success;

    private String callbackBody;

    private LocalDateTime createTime;
}
