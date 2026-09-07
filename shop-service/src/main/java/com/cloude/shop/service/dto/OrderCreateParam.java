package com.cloude.shop.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 下单参数
 */
@Data
public class OrderCreateParam {

    /** 订单商品（支持直接购买与购物车结算） */
    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemParam> items;

    @NotBlank(message = "收货人不能为空")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String receiverAddr;

    private String note;
}
