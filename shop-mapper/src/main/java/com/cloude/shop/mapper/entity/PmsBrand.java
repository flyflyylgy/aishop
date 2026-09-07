package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 品牌
 */
@Data
@TableName("pms_brand")
public class PmsBrand {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String logo;

    private String story;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;
}
