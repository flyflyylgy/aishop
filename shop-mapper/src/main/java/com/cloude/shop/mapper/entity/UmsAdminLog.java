package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理操作审计日志
 */
@Data
@TableName("ums_admin_log")
public class UmsAdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    /** 操作权限编码 */
    private String operation;

    private String method;

    private String path;

    /** 请求参数 JSON（截断） */
    private String params;

    private String ip;

    /** 0-失败 1-成功 */
    private Integer success;

    private String errorMsg;

    private LocalDateTime createTime;
}
