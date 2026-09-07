package com.cloude.shop.common.api;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    VALIDATE_FAILED(400, "参数校验失败"),
    STOCK_NOT_ENOUGH(600, "商品库存不足"),
    ORDER_STATE_ILLEGAL(601, "订单状态不允许该操作"),
    PAY_FAILED(602, "支付失败");

    private final long code;
    private final String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }
}
