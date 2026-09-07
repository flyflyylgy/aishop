package com.cloude.shop.service.dto;

import com.cloude.shop.mapper.entity.PmsProduct;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目 VO（含商品信息快照）
 */
@Data
public class CartVO {

    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    /** 当前售价（实时） */
    private BigDecimal price;

    /** 加入时价格 */
    private BigDecimal addedPrice;

    private Integer quantity;

    private Integer selected;

    /** 可售库存（前端用于校验） */
    private Integer availableStock;

    public static CartVO of(com.cloude.shop.mapper.entity.OmsCartItem item, PmsProduct product) {
        CartVO vo = new CartVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setAddedPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setPrice(product.getPrice());
            vo.setAvailableStock(product.getAvailableStock());
        }
        return vo;
    }
}
