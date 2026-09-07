package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.mapper.entity.UmsAdminRole;
import com.cloude.shop.mapper.entity.UmsPermission;
import com.cloude.shop.mapper.entity.UmsRole;
import com.cloude.shop.mapper.entity.UmsRolePermission;
import com.cloude.shop.mapper.mapper.UmsAdminRoleMapper;
import com.cloude.shop.mapper.mapper.UmsPermissionMapper;
import com.cloude.shop.mapper.mapper.UmsRoleMapper;
import com.cloude.shop.mapper.mapper.UmsRolePermissionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 服务：管理员权限集查询（Redis 缓存 10 分钟）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private final UmsAdminRoleMapper adminRoleMapper;
    private final UmsRolePermissionMapper rolePermissionMapper;
    private final UmsPermissionMapper permissionMapper;
    private final UmsRoleMapper roleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 查询管理员权限编码集合（含空集缓存防穿透，TTL 10 分钟）
     */
    public Set<String> loadPermissions(Long adminId) {
        String key = RedisKeyConstant.ADMIN_PERM_PREFIX + adminId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            }
        } catch (Exception ignore) {
        }
        Set<String> perms = loadFromDb(adminId);
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(perms), Duration.ofMinutes(10));
        } catch (Exception ignore) {
        }
        return perms;
    }

    private Set<String> loadFromDb(Long adminId) {
        List<Long> roleIds = adminRoleMapper.selectList(new LambdaQueryWrapper<UmsAdminRole>()
                        .eq(UmsAdminRole::getAdminId, adminId)).stream()
                .map(UmsAdminRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 超级管理员：注入通配权限 *，新增权限点对超管即时生效，无需重新绑定/清缓存
        List<UmsRole> roles = roleMapper.selectBatchIds(roleIds);
        boolean superAdmin = roles.stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
        if (superAdmin) {
            return new java.util.HashSet<>(java.util.List.of(com.cloude.shop.common.constant.PermissionCode.ALL));
        }
        List<Long> permIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<UmsRolePermission>()
                        .in(UmsRolePermission::getRoleId, roleIds)).stream()
                .map(UmsRolePermission::getPermissionId).toList();
        if (permIds.isEmpty()) {
            return Collections.emptySet();
        }
        return permissionMapper.selectBatchIds(permIds).stream()
                .map(UmsPermission::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * 确保管理员已绑定指定角色（未绑定则绑定）
     */
    public void ensureRole(Long adminId, String roleCode) {
        UmsRole role = roleMapper.selectOne(new LambdaQueryWrapper<UmsRole>()
                .eq(UmsRole::getCode, roleCode));
        if (role == null) {
            log.warn("角色 {} 不存在，跳过绑定", roleCode);
            return;
        }
        long count = adminRoleMapper.selectCount(new LambdaQueryWrapper<UmsAdminRole>()
                .eq(UmsAdminRole::getAdminId, adminId)
                .eq(UmsAdminRole::getRoleId, role.getId()));
        if (count == 0) {
            UmsAdminRole adminRole = new UmsAdminRole();
            adminRole.setAdminId(adminId);
            adminRole.setRoleId(role.getId());
            adminRoleMapper.insert(adminRole);
        }
        stringRedisTemplate.delete(RedisKeyConstant.ADMIN_PERM_PREFIX + adminId);
    }

    /**
     * 角色列表
     */
    public List<UmsRole> listRoles() {
        return roleMapper.selectList(null);
    }

    /**
     * 全部权限点（供角色授权勾选）
     */
    public List<UmsPermission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<UmsPermission>()
                .orderByAsc(UmsPermission::getId));
    }

    /**
     * 查询角色已绑定的权限ID集合
     */
    public Set<Long> rolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<UmsRolePermission>()
                        .eq(UmsRolePermission::getRoleId, roleId)).stream()
                .map(UmsRolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    /**
     * 保存角色权限（全量覆盖），并失效该角色下所有管理员的权限缓存
     */
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new com.cloude.shop.common.exception.BusinessException("角色不存在");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<UmsRolePermission>()
                .eq(UmsRolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            for (Long permId : permissionIds.stream().distinct().toList()) {
                UmsRolePermission rp = new UmsRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        evictCacheByRole(roleId);
    }

    /**
     * 失效绑定了指定角色的所有管理员权限缓存
     */
    private void evictCacheByRole(Long roleId) {
        List<Long> adminIds = adminRoleMapper.selectList(new LambdaQueryWrapper<UmsAdminRole>()
                        .eq(UmsAdminRole::getRoleId, roleId)).stream()
                .map(UmsAdminRole::getAdminId).toList();
        adminIds.forEach(id -> stringRedisTemplate.delete(RedisKeyConstant.ADMIN_PERM_PREFIX + id));
    }
}
