package com.cloude.shop.admin.controller;

import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.service.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台首页统计聚合接口（登录即可访问，无 RBAC 权限要求）
 */
@Tag(name = "AdminStatsController", description = "首页统计")
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final DashboardService dashboardService;

    @Operation(summary = "首页 Dashboard 聚合统计")
    @GetMapping("/dashboard")
    public CommonResult<Map<String, Object>> dashboard() {
        return CommonResult.success(dashboardService.dashboard());
    }
}
