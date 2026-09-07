package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU（多规格）
 * 状态：0-禁用 1-启用
 */
@Data
@TableName("pms_sku")
public class PmsSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String skuCode;

    /** 规格值 JSON：{"颜色":"红","内存":"8G"} */
    private String specValues;

    private BigDecimal price;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer soldStock;

    /** 0-禁用 1-启用 */
    private Integer status;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
