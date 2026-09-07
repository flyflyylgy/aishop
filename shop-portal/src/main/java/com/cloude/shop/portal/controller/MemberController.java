package com.cloude.shop.portal.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.service.dto.MemberVO;
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
