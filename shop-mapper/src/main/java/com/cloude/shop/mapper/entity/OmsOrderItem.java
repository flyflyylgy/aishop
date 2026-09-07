package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细（下单时商品快照）
 */
@Data
@TableName("oms_order_item")
public class OmsOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long productId;

    private String productPic;

    /** 商品名称（下单快照） */
    private String productName;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalPrice;

    private LocalDateTime createTime;
}
