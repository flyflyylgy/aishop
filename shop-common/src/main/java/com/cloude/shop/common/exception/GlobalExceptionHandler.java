package com.cloude.shop.common.exception;

import com.cloude.shop.common.api.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return CommonResult.failed(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public CommonResult<Void> handleValidException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException ex) {
            message = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                    .findFirst().orElse("参数校验失败");
        } else if (e instanceof BindException ex) {
            message = ex.getFieldErrors().stream()
                    .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                    .findFirst().orElse("参数校验失败");
        } else {
            message = "参数校验失败";
        }
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResult<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return CommonResult.validateFailed("请求体格式错误");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResult<Void> handleDuplicateKey(DuplicateKeyException e) {
        return CommonResult.failed("数据重复，请勿重复提交");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonResult<Void> handleNotFound(NoHandlerFoundException e) {
        return CommonResult.failed(404, "接口不存在: " + e.getRequestURL());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResult.failed("系统繁忙，请稍后重试");
    }
}
