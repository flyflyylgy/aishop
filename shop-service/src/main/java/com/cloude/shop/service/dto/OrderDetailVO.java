package com.cloude.shop.service.dto;

import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.OmsOrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 订单详情 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OmsOrder {

    /** 订单明细快照 */
    private List<OmsOrderItem> items;

    /** 状态中文描述（组装时赋值） */
    private String statusDesc;
}
