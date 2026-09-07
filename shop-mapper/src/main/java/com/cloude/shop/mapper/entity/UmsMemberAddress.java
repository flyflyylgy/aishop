package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员收货地址
 */
@Data
@TableName("ums_member_address")
public class UmsMemberAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddr;

    /** 0-普通 1-默认地址 */
    private Integer isDefault;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
