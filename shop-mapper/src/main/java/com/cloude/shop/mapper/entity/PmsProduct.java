package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（库存三态分离：available / locked / sold）
 */
@Data
@TableName("pms_product")
public class PmsProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Long brandId;

    private String name;

    private String subTitle;

    private String mainImage;

    /** 副图 URL，逗号分隔 */
    private String subImages;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String detailHtml;

    /** 可售库存 */
    private Integer availableStock;

    /** 锁定库存（已下单未支付） */
    private Integer lockedStock;

    /** 已售库存 */
    private Integer soldStock;

    /** 销量（冗余展示） */
    private Integer sale;

    /** 是否多规格:0-否 1-是 */
    private Integer hasSku;

    /** 规格维度名(逗号分隔:颜色,内存) */
    private String specNames;

    /** 0-下架 1-上架 */
    private Integer status;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
