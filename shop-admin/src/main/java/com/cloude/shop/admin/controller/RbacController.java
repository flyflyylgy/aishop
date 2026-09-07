package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.mapper.entity.UmsAdminLog;
import com.cloude.shop.mapper.entity.UmsMemberLog;
import com.cloude.shop.mapper.entity.UmsPermission;
import com.cloude.shop.mapper.entity.UmsRole;
import com.cloude.shop.mapper.mapper.UmsAdminLogMapper;
import com.cloude.shop.mapper.mapper.UmsMemberLogMapper;
import com.cloude.shop.service.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * RBAC 查询：角色列表 / 审计日志分页
 */
@Tag(name = "RbacController", description = "角色与审计日志")
@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;
    private final UmsAdminLogMapper adminLogMapper;
    private final UmsMemberLogMapper memberLogMapper;

    @Operation(summary = "角色列表")
    @RequirePermission(PermissionCode.RBAC_LIST)
    @GetMapping("/role/list")
    public CommonResult<List<UmsRole>> roles() {
        return CommonResult.success(rbacService.listRoles());
    }

    @Operation(summary = "全部权限点（授权勾选）")
    @RequirePermission(PermissionCode.RBAC_LIST)
    @GetMapping("/permission/list")
    public CommonResult<List<UmsPermission>> permissions() {
        return CommonResult.success(rbacService.listPermissions());
    }

    @Operation(summary = "查询角色已绑定权限ID")
    @RequirePermission(PermissionCode.RBAC_LIST)
    @GetMapping("/role/permissions")
    public CommonResult<Set<Long>> rolePermissions(@RequestParam Long roleId) {
        return CommonResult.success(rbacService.rolePermissionIds(roleId));
    }

    @Operation(summary = "保存角色权限（全量覆盖，实时生效）")
    @OpLog("ROLE_PERM_SAVE")
    @RequirePermission(PermissionCode.ROLE_ASSIGN)
    @PostMapping("/role/permissions")
    public CommonResult<Void> saveRolePermissions(@RequestBody RolePermParam param) {
        rbacService.saveRolePermissions(param.getRoleId(), param.getPermissionIds());
        return CommonResult.success();
    }

    @Data
    public static class RolePermParam {
        private Long roleId;
        private List<Long> permissionIds;
    }

    @Operation(summary = "管理操作审计日志分页")
    @RequirePermission(PermissionCode.RBAC_LIST)
    @GetMapping("/log/page")
    public CommonResult<Page<UmsAdminLog>> logs(@RequestParam(required = false) Long adminId,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(adminLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsAdminLog>()
                        .eq(adminId != null, UmsAdminLog::getAdminId, adminId)
                        .orderByDesc(UmsAdminLog::getId)));
    }

    @Operation(summary = "会员业务操作日志分页（登录/注册/下单/支付/取消/收货等）")
    @RequirePermission(PermissionCode.MEMBER_LOG)
    @GetMapping("/member-log/page")
    public CommonResult<Page<UmsMemberLog>> memberLogs(@RequestParam(required = false) Long memberId,
                                                       @RequestParam(required = false) String operation,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(memberLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsMemberLog>()
                        .eq(memberId != null, UmsMemberLog::getMemberId, memberId)
                        .eq(operation != null && !operation.isBlank(), UmsMemberLog::getOperation, operation)
                        .orderByDesc(UmsMemberLog::getId)));
    }
}
