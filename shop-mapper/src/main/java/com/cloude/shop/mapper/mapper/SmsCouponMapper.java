package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.SmsCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券 Mapper —— 领取/使用数量 CAS 变更
 */
public interface SmsCouponMapper extends BaseMapper<SmsCoupon> {

    /**
     * 领券：已领 +1（CAS，仅在上架且未领完时成功，affected=0 即已抢光/下架）
     */
    @Update("UPDATE sms_coupon SET received_count = received_count + 1 "
            + "WHERE id = #{id} AND status = 1 AND delete_flag = 0 AND received_count < total_count")
    int casIncreaseReceived(@Param("id") Long id);

    /**
     * 用券：已用 +1
     */
    @Update("UPDATE sms_coupon SET used_count = used_count + 1 WHERE id = #{id}")
    int increaseUsed(@Param("id") Long id);

    /**
     * 关单回退：已用 -1（不小于 0 兜底）
     */
    @Update("UPDATE sms_coupon SET used_count = used_count - 1 WHERE id = #{id} AND used_count > 0")
    int decreaseUsed(@Param("id") Long id);
}
