package com.cloude.shop.admin.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.service.component.CaptchaService;
import com.cloude.shop.service.dto.TokenVO;
import com.cloude.shop.service.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台认证接口
 */
@Tag(name = "AdminAuthController", description = "后台管理员认证")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;
    private final CaptchaService captchaService;

    @Operation(summary = "图形验证码")
    @GetMapping("/captcha")
    public CommonResult<Map<String, String>> captcha() {
        return CommonResult.success(captchaService.generate());
    }

    @Operation(summary = "管理员登录")
    @OpLog("ADMIN_LOGIN")
    @PostMapping("/login")
    public CommonResult<TokenVO> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(adminService.login(param.getUsername(), param.getPassword(),
                param.getCaptchaKey(), param.getCaptchaCode()));
    }

    @Operation(summary = "退出登录")
    @OpLog("ADMIN_LOGOUT")
    @PostMapping("/logout")
    public CommonResult<Void> logout(HttpServletRequest request) {
        adminService.logout(request.getHeader("Authorization"));
        return CommonResult.success();
    }

    @Operation(summary = "修改密码（首次登录后必须更换默认口令）")
    @OpLog("ADMIN_CHANGE_PASSWORD")
    @PostMapping("/password")
    public CommonResult<Void> modifyPassword(@RequestBody ModifyPasswordParam param) {
        adminService.modifyPassword(com.cloude.shop.common.component.UserContext.getUserId(),
                param.getOldPassword(), param.getNewPassword());
        return CommonResult.success();
    }

    @Data
    public static class LoginParam {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "验证码不能为空")
        private String captchaKey;
        @NotBlank(message = "验证码不能为空")
        private String captchaCode;
    }

    @Data
    public static class ModifyPasswordParam {
        private String oldPassword;
        private String newPassword;
    }
}
