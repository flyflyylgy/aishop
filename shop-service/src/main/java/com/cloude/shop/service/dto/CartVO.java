package com.cloude.shop.service.dto;

import com.cloude.shop.mapper.entity.OmsCartItem;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsSku;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目 VO（含商品信息快照）
 */
@Data
public class CartVO {

    private Long id;

    private Long productId;

    private Long skuId;

    private String productName;

    private String productImage;

    /** 规格快照（如 颜色:红;内存:8G） */
    private String specValues;

    /** 当前售价（实时） */
    private BigDecimal price;

    /** 加入时价格 */
    private BigDecimal addedPrice;

    private Integer quantity;

    private Integer selected;

    /** 可售库存（前端用于校验） */
    private Integer availableStock;

    public static CartVO of(OmsCartItem item, PmsProduct product, PmsSku sku) {
        CartVO vo = new CartVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setAddedPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
        }
        if (sku != null) {
            vo.setSpecValues(sku.getSpecValues());
            vo.setPrice(sku.getPrice());
            vo.setAvailableStock(sku.getAvailableStock());
        } else if (product != null) {
            vo.setPrice(product.getPrice());
            vo.setAvailableStock(product.getAvailableStock());
        }
        return vo;
    }
}
