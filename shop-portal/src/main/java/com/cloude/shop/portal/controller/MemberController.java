package com.cloude.shop.portal.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.dto.ForgotPasswordParam;
import com.cloude.shop.service.dto.MemberVO;
import com.cloude.shop.service.dto.ProfileUpdateParam;
import com.cloude.shop.service.dto.ResetPasswordParam;
import com.cloude.shop.service.dto.TokenVO;
import com.cloude.shop.service.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会员接口
 */
@Tag(name = "MemberController", description = "会员注册登录")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final com.cloude.shop.service.component.CaptchaService captchaService;

    @Operation(summary = "注册")
    @OpLog("MEMBER_REGISTER")
    @PostMapping("/register")
    public CommonResult<Void> register(@Valid @RequestBody RegisterParam param) {
        memberService.register(param.getUsername(), param.getPassword(), param.getNickname());
        return CommonResult.success();
    }

    @Operation(summary = "图形验证码")
    @GetMapping("/captcha")
    public CommonResult<java.util.Map<String, String>> captcha() {
        return CommonResult.success(captchaService.generate());
    }

    @Operation(summary = "登录")
    @OpLog("MEMBER_LOGIN")
    @PostMapping("/login")
    public CommonResult<TokenVO> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(memberService.login(param.getUsername(), param.getPassword(),
                param.getCaptchaKey(), param.getCaptchaCode()));
    }

    @Operation(summary = "当前会员信息")
    @GetMapping("/info")
    public CommonResult<MemberVO> info() {
        return CommonResult.success(memberService.info());
    }

    @Operation(summary = "退出登录")
    @OpLog("MEMBER_LOGOUT")
    @PostMapping("/logout")
    public CommonResult<Void> logout(HttpServletRequest request) {
        memberService.logout(request.getHeader("Authorization"));
        return CommonResult.success();
    }

    @Operation(summary = "修改个人资料（昵称/手机/头像）")
    @OpLog("MEMBER_PROFILE_UPDATE")
    @PostMapping("/profile")
    public CommonResult<Void> updateProfile(@Valid @RequestBody ProfileUpdateParam param) {
        memberService.updateProfile(UserContext.getUserId(), param);
        return CommonResult.success();
    }

    // ==================== 忘记密码（免登录） ====================

    @Operation(summary = "发送重置密码验证码（演示环境返回验证码）")
    @PostMapping("/forgot/send-code")
    public CommonResult<String> sendResetCode(@Valid @RequestBody ForgotPasswordParam param) {
        String code = memberService.sendResetCode(param.getUsername());
        return CommonResult.success(code);
    }

    @Operation(summary = "验证码重置密码")
    @PostMapping("/forgot/reset-password")
    public CommonResult<Void> resetPassword(@Valid @RequestBody ResetPasswordParam param) {
        memberService.resetPassword(param.getUsername(), param.getCode(), param.getNewPassword());
        return CommonResult.success();
    }

    @Data
    public static class RegisterParam {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String nickname;
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
}
