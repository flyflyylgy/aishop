package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员站内信
 * 类型：1-订单 2-支付 3-物流 4-售后 5-系统
 */
@Data
@TableName("ums_message")
public class UmsMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Integer type;

    private String title;

    private String content;

    /** 业务类型：order / refund */
    private String bizType;

    /** 业务 ID（订单号等） */
    private String bizId;

    /** 0-未读 1-已读 */
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
