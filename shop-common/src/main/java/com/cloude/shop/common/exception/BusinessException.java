package com.cloude.shop.common.exception;

import com.cloude.shop.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常（如：库存不足、订单状态非法）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final long code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(long code, String message) {
        super(message);
        this.code = code;
    }
}
