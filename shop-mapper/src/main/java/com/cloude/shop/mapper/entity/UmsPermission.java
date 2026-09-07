package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限点
 */
@Data
@TableName("ums_permission")
public class UmsPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private LocalDateTime createTime;
}
