package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车
 */
@Data
@TableName("oms_cart_item")
public class OmsCartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long productId;

    private Integer quantity;

    /** 加入时价格 */
    private BigDecimal price;

    /** 0-未勾选 1-勾选 */
    private Integer selected;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
