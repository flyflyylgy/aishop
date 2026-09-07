package com.cloude.shop.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类
 */
@Data
@TableName("pms_category")
public class PmsCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID，0 为一级分类 */
    private Long parentId;

    private String name;

    private Integer sort;

    /** 0-隐藏 1-显示 */
    private Integer showFlag;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;
}
