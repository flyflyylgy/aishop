package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员业务操作日志
 */
@Data
@TableName("ums_member_log")
public class UmsMemberLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /** 会员账号（冗余，登录失败时也可追溯） */
    private String username;

    /** 操作类型，如 MEMBER_LOGIN / ORDER_CREATE */
    private String operation;

    private String method;

    private String path;

    /** 请求参数 JSON（敏感字段脱敏、截断） */
    private String params;

    private String ip;

    /** 0-失败 1-成功 */
    private Integer success;

    private String errorMsg;

    private LocalDateTime createTime;
}
