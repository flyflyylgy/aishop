package com.cloude.shop.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码——验证码重置密码
 */
@Data
public class ResetPasswordParam {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码需 8-32 位")
    private String newPassword;
}
