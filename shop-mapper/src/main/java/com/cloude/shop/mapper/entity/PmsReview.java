package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价
 */
@Data
@TableName("pms_review")
public class PmsReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private Long memberId;
    private String memberName;
    private Long orderId;
    private String orderNo;
    private Integer rating;
    private String content;
    private String pics;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;
}
