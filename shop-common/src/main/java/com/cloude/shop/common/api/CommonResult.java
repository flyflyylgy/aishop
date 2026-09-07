package com.cloude.shop.common.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应封装
 */
@Data
public class CommonResult<T> implements Serializable {

    private long code;
    private String message;
    private T data;

    protected CommonResult() {
    }

    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success() {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(ResultCode.FAILED.getCode(), message, null);
    }

    public static <T> CommonResult<T> failed(ResultCode resultCode) {
        return new CommonResult<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> CommonResult<T> failed(long code, String message) {
        return new CommonResult<>(code, message, null);
    }

    public static <T> CommonResult<T> unauthorized(String message) {
        return new CommonResult<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    public static <T> CommonResult<T> forbidden(String message) {
        return new CommonResult<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }

    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }
}
