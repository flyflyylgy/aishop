package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.common.util.JwtUtil;
import com.cloude.shop.mapper.entity.UmsAdmin;
import com.cloude.shop.mapper.entity.UmsAdminRole;
import com.cloude.shop.mapper.entity.UmsRole;
import com.cloude.shop.mapper.mapper.UmsAdminMapper;
import com.cloude.shop.mapper.mapper.UmsAdminRoleMapper;
import com.cloude.shop.service.component.CaptchaService;
import com.cloude.shop.service.component.LoginGuard;
import com.cloude.shop.service.dto.TokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 后台管理员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,32}$");

    private final UmsAdminMapper adminMapper;
    private final UmsAdminRoleMapper adminRoleMapper;
    private final RbacService rbacService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final LoginGuard loginGuard;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 初始化默认管理员（首次启动：admin / admin123，仅当表为空时）
     */
    public void initDefaultAdmin() {
        long count = adminMapper.selectCount(null);
        if (count == 0) {
            UmsAdmin admin = new UmsAdmin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickName("超级管理员");
            admin.setStatus(1);
            adminMapper.insert(admin);
            rbacService.ensureRole(admin.getId(), "SUPER_ADMIN");
            log.warn("==============================================================");
            log.warn("已初始化默认管理员 admin / admin123，请立即登录并修改密码！");
            log.warn("==============================================================");
        }
    }

    /**
     * 修改管理员密码（校验原密码 + 新密码强度）
     */
    public void modifyPassword(Long adminId, String oldPassword, String newPassword) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        validateStrongPassword(newPassword);
        if (passwordEncoder.matches(newPassword, admin.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        UmsAdmin update = new UmsAdmin();
        update.setId(adminId);
        update.setPassword(passwordEncoder.encode(newPassword));
        adminMapper.updateById(update);
    }

    public TokenVO login(String username, String password, String captchaKey, String captchaCode) {
        if (!captchaService.verify(captchaKey, captchaCode)) {
            throw new BusinessException("验证码错误或已失效");
        }
        loginGuard.assertNotLocked("admin", username);
        UmsAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<UmsAdmin>()
                .eq(UmsAdmin::getUsername, username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            loginGuard.recordFailure("admin", username);
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系超级管理员");
        }
        loginGuard.clearFailures("admin", username);
        String token = jwtUtil.generate(admin.getId(), "admin", admin.getUsername());
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.ADMIN_TOKEN + token, admin.getUsername(),
                Duration.ofSeconds(RedisKeyConstant.TOKEN_EXPIRE_SECONDS));
        Set<String> perms = rbacService.loadPermissions(admin.getId());
        return new TokenVO(token, admin.getId(), admin.getUsername(), "admin", perms);
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            stringRedisTemplate.delete(RedisKeyConstant.ADMIN_TOKEN + token.substring(7));
        }
    }

    // ==================== 管理员账号管理 ====================

    /**
     * 管理员分页（含角色ID/角色名，不返回密码）
     */
    public Page<Map<String, Object>> page(String keyword, Integer pageNum, Integer pageSize) {
        Page<UmsAdmin> page = adminMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsAdmin>()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(UmsAdmin::getUsername, keyword.trim())
                                .or().like(UmsAdmin::getNickName, keyword.trim()))
                        .orderByAsc(UmsAdmin::getId));
        List<Long> adminIds = page.getRecords().stream().map(UmsAdmin::getId).toList();
        Map<Long, List<UmsRole>> roleMap = rolesOfAdmins(adminIds);
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("username", a.getUsername());
            row.put("nickName", a.getNickName());
            row.put("status", a.getStatus());
            row.put("createTime", a.getCreateTime());
            List<UmsRole> roles = roleMap.getOrDefault(a.getId(), List.of());
            row.put("roleIds", roles.stream().map(UmsRole::getId).toList());
            row.put("roleNames", roles.stream().map(UmsRole::getName).toList());
            return row;
        }).collect(Collectors.toList()));
        return result;
    }

    public void createAdmin(String username, String password, String nickName) {
        if (username == null || username.isBlank() || username.length() < 3) {
            throw new BusinessException("用户名至少 3 个字符");
        }
        validateStrongPassword(password);
        long count = adminMapper.selectCount(new LambdaQueryWrapper<UmsAdmin>()
                .eq(UmsAdmin::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername(username.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickName(nickName);
        admin.setStatus(1);
        adminMapper.insert(admin);
    }

    /**
     * 编辑管理员（昵称/状态）；禁止禁用自己
     */
    public void updateAdmin(Long operatorId, Long adminId, String nickName, Integer status) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (status != null && status == 0 && adminId.equals(operatorId)) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        UmsAdmin update = new UmsAdmin();
        update.setId(adminId);
        if (nickName != null) {
            update.setNickName(nickName);
        }
        if (status != null) {
            update.setStatus(status);
        }
        adminMapper.updateById(update);
    }

    /**
     * 重置管理员密码
     */
    public void resetPassword(Long adminId, String newPassword) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        validateStrongPassword(newPassword);
        UmsAdmin update = new UmsAdmin();
        update.setId(adminId);
        update.setPassword(passwordEncoder.encode(newPassword));
        adminMapper.updateById(update);
    }

    /**
     * 分配角色（全量覆盖），失效权限缓存
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long adminId, List<Long> roleIds) {
        if (adminMapper.selectById(adminId) == null) {
            throw new BusinessException("管理员不存在");
        }
        adminRoleMapper.delete(new LambdaQueryWrapper<UmsAdminRole>()
                .eq(UmsAdminRole::getAdminId, adminId));
        if (roleIds != null) {
            for (Long roleId : roleIds.stream().distinct().toList()) {
                UmsAdminRole ar = new UmsAdminRole();
                ar.setAdminId(adminId);
                ar.setRoleId(roleId);
                adminRoleMapper.insert(ar);
            }
        }
        stringRedisTemplate.delete(RedisKeyConstant.ADMIN_PERM_PREFIX + adminId);
    }

    private void validateStrongPassword(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException("密码需 8-32 位且包含字母和数字");
        }
    }

    private Map<Long, List<UmsRole>> rolesOfAdmins(List<Long> adminIds) {
        if (adminIds.isEmpty()) {
            return Map.of();
        }
        List<UmsAdminRole> bindings = adminRoleMapper.selectList(new LambdaQueryWrapper<UmsAdminRole>()
                .in(UmsAdminRole::getAdminId, adminIds));
        if (bindings.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = bindings.stream().map(UmsAdminRole::getRoleId).distinct().toList();
        Map<Long, UmsRole> roleMap = rbacService.listRoles().stream()
                .filter(r -> roleIds.contains(r.getId()))
                .collect(Collectors.toMap(UmsRole::getId, r -> r));
        Map<Long, List<UmsRole>> result = new HashMap<>();
        for (UmsAdminRole b : bindings) {
            UmsRole role = roleMap.get(b.getRoleId());
            if (role != null) {
                result.computeIfAbsent(b.getAdminId(), k -> new java.util.ArrayList<>()).add(role);
            }
        }
        return result;
    }
}
