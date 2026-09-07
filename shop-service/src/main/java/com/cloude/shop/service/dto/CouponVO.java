package com.cloude.shop.service.dto;

import com.cloude.shop.mapper.entity.SmsCoupon;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员视角的优惠券 VO（券模板 + 领券流水信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponVO extends SmsCoupon {

    /** 领券记录 ID（我的优惠券有值；领券中心为 null） */
    private Long historyId;

    /** 领券流水状态（0-未使用 1-已使用 2-已过期，已按 expireTime 修正） */
    private Integer historyStatus;

    private LocalDateTime receiveTime;

    private LocalDateTime expireTime;

    private LocalDateTime useTime;

    private String orderNo;

    /** 领券中心：当前会员是否已达限领张数 */
    private Boolean claimed;

    /** 领券中心：剩余可领张数 */
    private Integer remaining;
}
