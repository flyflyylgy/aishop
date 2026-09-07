package com.cloude.shop.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 忘记密码——发送重置验证码
 */
@Data
public class ForgotPasswordParam {

    @NotBlank(message = "用户名不能为空")
    private String username;
}
