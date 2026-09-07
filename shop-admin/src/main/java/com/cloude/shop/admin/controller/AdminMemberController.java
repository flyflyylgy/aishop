package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.service.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台会员管理
 */
@Tag(name = "AdminMemberController", description = "会员管理")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @Operation(summary = "会员分页（含订单数/累计消费）")
    @RequirePermission(PermissionCode.MEMBER_LIST)
    @GetMapping("/page")
    public CommonResult<Page<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(memberService.adminPage(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "封禁/解封会员（封禁即吊销登录态）")
    @OpLog("MEMBER_STATUS_CHANGE")
    @RequirePermission(PermissionCode.MEMBER_STATUS)
    @PostMapping("/status/{id}/{status}")
    public CommonResult<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        memberService.updateStatus(id, status);
        return CommonResult.success();
    }
}
