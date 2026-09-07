package com.cloude.shop.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调参数（模拟第三方支付回调报文）
 */
@Data
public class PayNotifyParam {

    @NotBlank(message = "支付流水号不能为空")
    private String payNo;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    private Boolean success = true;

    private String body;
}
