package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.OmsOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 Mapper —— 订单状态 CAS 流转
 * 状态更新全部带前置状态条件（affected=0 表示并发竞争/状态非法）
 */
public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

    /**
     * CAS：待付款 -> 已付款（支付幂等核心，只允许一次成功）
     */
    @Update("UPDATE oms_order SET status = 1, pay_type = #{payType}, pay_time = #{payTime} "
            + "WHERE order_no = #{orderNo} AND status = 0")
    int casMarkPaid(@Param("orderNo") String orderNo,
                    @Param("payType") Integer payType,
                    @Param("payTime") LocalDateTime payTime);

    /**
     * CAS：待付款 -> 已关闭（关单：超时/取消）
     */
    @Update("UPDATE oms_order SET status = 4, close_time = #{closeTime}, close_reason = #{reason} "
            + "WHERE order_no = #{orderNo} AND status = 0")
    int casClosePendingOrder(@Param("orderNo") String orderNo,
                             @Param("closeTime") LocalDateTime closeTime,
                             @Param("reason") String reason);

    /**
     * CAS：已付款 -> 已发货（后台发货，含物流公司与运单号）
     */
    @Update("UPDATE oms_order SET status = 2, ship_time = #{shipTime}, "
            + "express_company = #{expressCompany}, express_no = #{expressNo} "
            + "WHERE id = #{orderId} AND status = 1")
    int casShip(@Param("orderId") Long orderId,
                @Param("shipTime") LocalDateTime shipTime,
                @Param("expressCompany") String expressCompany,
                @Param("expressNo") String expressNo);

    /**
     * CAS：已发货 -> 已完成（会员确认收货）
     */
    @Update("UPDATE oms_order SET status = 3, finish_time = #{finishTime} "
            + "WHERE id = #{orderId} AND status = 2")
    int casComplete(@Param("orderId") Long orderId, @Param("finishTime") LocalDateTime finishTime);

    /**
     * 已支付订单销售额合计（状态 1/2/3）
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM oms_order WHERE status IN (1, 2, 3)")
    BigDecimal sumPaidAmount();

    /**
     * 指定时间起已支付订单销售额合计
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM oms_order WHERE pay_time IS NOT NULL AND pay_time >= #{start}")
    BigDecimal sumPaidAmountSince(@Param("start") LocalDateTime start);
}
