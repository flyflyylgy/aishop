package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员
 */
@Data
@TableName("ums_admin")
public class UmsAdmin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickName;

    /** 0-禁用 1-启用 */
    private Integer status;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
