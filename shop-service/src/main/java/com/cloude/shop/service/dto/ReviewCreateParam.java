package com.cloude.shop.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 发表评价参数
 */
@Data
public class ReviewCreateParam {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 500, message = "评价内容最长 500 字")
    private String content;

    @Size(max = 1000, message = "图片 URL 总长超限")
    private String pics;
}
