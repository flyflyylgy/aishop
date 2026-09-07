package com.cloude.shop.common.enums;

import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机
 * PENDING_PAYMENT(待付款) -> PAID(已付款) -> SHIPPED(已发货) -> COMPLETED(已完成)
 * PENDING_PAYMENT -> CLOSED(已关闭)（用户取消/超时关单）
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待付款"),
    PAID(1, "已付款"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }

    /**
     * 基于 EnumMap 的状态转换表：所有流转必须经过此表校验
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITION_TABLE = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITION_TABLE.put(PENDING_PAYMENT, Set.of(PAID, CLOSED));
        TRANSITION_TABLE.put(PAID, Set.of(SHIPPED, CLOSED));
        TRANSITION_TABLE.put(SHIPPED, Set.of(COMPLETED));
        TRANSITION_TABLE.put(COMPLETED, Set.of());
        TRANSITION_TABLE.put(CLOSED, Set.of());
    }

    /**
     * 校验当前状态能否流转到目标状态
     */
    public boolean canTransferTo(OrderStatus target) {
        return TRANSITION_TABLE.getOrDefault(this, Set.of()).contains(target);
    }
}
