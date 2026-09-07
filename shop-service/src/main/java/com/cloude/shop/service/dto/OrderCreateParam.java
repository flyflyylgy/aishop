package com.cloude.shop.service.dto;

import jakarta.validation.Valid;
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

    /** 地址簿 ID（提供时自动填充收货人信息，优先于手填字段） */
    private Long addressId;

    /** 以下三个字段在未传 addressId 时必填 */
    private String receiverName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    private String receiverAddr;

    private String note;
}
