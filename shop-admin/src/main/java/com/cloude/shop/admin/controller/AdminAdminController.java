package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.service.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员账号管理
 */
@Tag(name = "AdminAdminController", description = "管理员账号管理")
@RestController
@RequestMapping("/admin-user")
@RequiredArgsConstructor
public class AdminAdminController {

    private final AdminService adminService;

    @Operation(summary = "管理员分页")
    @RequirePermission(PermissionCode.ADMIN_LIST)
    @GetMapping("/page")
    public CommonResult<Page<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(adminService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增管理员")
    @OpLog("ADMIN_CREATE")
    @RequirePermission(PermissionCode.ADMIN_CREATE)
    @PostMapping("/create")
    public CommonResult<Void> create(@Valid @RequestBody CreateAdminParam param) {
        adminService.createAdmin(param.getUsername(), param.getPassword(), param.getNickName());
        return CommonResult.success();
    }

    @Operation(summary = "编辑管理员（昵称/状态）")
    @OpLog("ADMIN_UPDATE")
    @RequirePermission(PermissionCode.ADMIN_UPDATE)
    @PostMapping("/update/{id}")
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody UpdateAdminParam param) {
        adminService.updateAdmin(UserContext.getUserId(), id, param.getNickName(), param.getStatus());
        return CommonResult.success();
    }

    @Operation(summary = "重置管理员密码")
    @OpLog("ADMIN_RESET_PASSWORD")
    @RequirePermission(PermissionCode.ADMIN_UPDATE)
    @PostMapping("/reset-password/{id}")
    public CommonResult<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordParam param) {
        adminService.resetPassword(id, param.getNewPassword());
        return CommonResult.success();
    }

    @Operation(summary = "分配角色")
    @OpLog("ADMIN_ASSIGN_ROLE")
    @RequirePermission(PermissionCode.ADMIN_ASSIGN)
    @PostMapping("/roles/{id}")
    public CommonResult<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRoleParam param) {
        adminService.assignRoles(id, param.getRoleIds());
        return CommonResult.success();
    }

    @Data
    public static class CreateAdminParam {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String nickName;
    }

    @Data
    public static class UpdateAdminParam {
        private String nickName;
        /** 0-禁用 1-启用 */
        private Integer status;
    }

    @Data
    public static class ResetPasswordParam {
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    public static class AssignRoleParam {
        private List<Long> roleIds;
    }
}
