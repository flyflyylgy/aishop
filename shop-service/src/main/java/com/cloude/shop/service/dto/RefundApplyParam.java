package com.cloude.shop.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 申请退款参数
 */
@Data
public class RefundApplyParam {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因最长 200 字")
    private String reason;
}
