package com.cloude.shop.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 下单商品项
 */
@Data
public class OrderItemParam {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** SKU ID（多规格商品必填，单规格商品为空） */
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    @Max(value = 999, message = "单商品最多购买999件")
    private Integer quantity;
}
