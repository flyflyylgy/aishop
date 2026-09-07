package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券（营销活动模板）
 * 类型：1-满减券 2-折扣券 3-现金券(无门槛)
 */
@Data
@TableName("sms_coupon")
public class SmsCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1-满减券 2-折扣券 3-现金券(无门槛) */
    private Integer type;

    /** 面额（满减/现金券抵扣金额） */
    private BigDecimal faceValue;

    /** 折扣（折扣券，如 85 表示 8.5 折，取值 1-99） */
    private Integer discount;

    /** 使用门槛金额（0=无门槛） */
    private BigDecimal minPoint;

    private Integer totalCount;

    private Integer receivedCount;

    private Integer usedCount;

    /** 每人限领张数 */
    private Integer perLimit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 0-下架 1-上架 */
    private Integer status;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
