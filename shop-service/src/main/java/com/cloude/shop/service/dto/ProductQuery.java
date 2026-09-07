package com.cloude.shop.service.dto;

import lombok.Data;

/**
 * 商品分页查询参数（后台）
 */
@Data
public class ProductQuery {

    private String keyword;

    private Long categoryId;

    /** 0-下架 1-上架，null 查全部 */
    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
