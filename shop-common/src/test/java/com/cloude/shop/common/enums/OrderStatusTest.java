package com.cloude.shop.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单状态机流转规则测试
 */
class OrderStatusTest {

    @Test
    void legalTransitions() {
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransferTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransferTo(OrderStatus.CLOSED));
        assertTrue(OrderStatus.PAID.canTransferTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PAID.canTransferTo(OrderStatus.CLOSED));
        assertTrue(OrderStatus.SHIPPED.canTransferTo(OrderStatus.COMPLETED));
    }

    @Test
    void illegalTransitions() {
        // 不能跳过支付直接发货/完成
        assertFalse(OrderStatus.PENDING_PAYMENT.canTransferTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.PENDING_PAYMENT.canTransferTo(OrderStatus.COMPLETED));
        // 不能从已付款回退待付款
        assertFalse(OrderStatus.PAID.canTransferTo(OrderStatus.PENDING_PAYMENT));
        // 终态不可流转
        assertFalse(OrderStatus.CLOSED.canTransferTo(OrderStatus.PENDING_PAYMENT));
        assertFalse(OrderStatus.COMPLETED.canTransferTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CLOSED.canTransferTo(OrderStatus.PAID));
    }

    @Test
    void ofReturnsCorrectEnum() {
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.of(0));
        assertEquals(OrderStatus.PAID, OrderStatus.of(1));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.of(2));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.of(3));
        assertEquals(OrderStatus.CLOSED, OrderStatus.of(4));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.of(99));
    }
}
