package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员-角色关联
 */
@Data
@TableName("ums_admin_role")
public class UmsAdminRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private Long roleId;
}
