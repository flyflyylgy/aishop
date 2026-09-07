package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.common.util.JwtUtil;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.UmsMember;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.UmsMemberMapper;
import com.cloude.shop.service.component.CaptchaService;
import com.cloude.shop.service.component.LoginGuard;
import com.cloude.shop.service.dto.MemberVO;
import com.cloude.shop.service.dto.ProfileUpdateParam;
import com.cloude.shop.service.dto.TokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 会员服务（注册/登录/登出/信息/个人资料/忘记密码 + 后台会员管理）
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    /** 密码强度：8-32 位，必须含字母和数字 */
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,32}$");

    private final UmsMemberMapper memberMapper;
    private final OmsOrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final LoginGuard loginGuard;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 重置密码验证码 Redis key 前缀 */
    private static final String RESET_CODE_PREFIX = "shop:resetpwd:";

    public void register(String username, String password, String nickname) {
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException("密码需 8-32 位且包含字母和数字");
        }
        long count = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        UmsMember member = new UmsMember();
        member.setUsername(username);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickname(nickname != null ? nickname : username);
        member.setStatus(1);
        memberMapper.insert(member);
    }

    public TokenVO login(String username, String password, String captchaKey, String captchaCode) {
        if (!captchaService.verify(captchaKey, captchaCode)) {
            throw new BusinessException("验证码错误或已失效");
        }
        loginGuard.assertNotLocked("member", username);
        UmsMember member = memberMapper.selectOne(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, username));
        if (member == null || !passwordEncoder.matches(password, member.getPassword())) {
            loginGuard.recordFailure("member", username);
            throw new BusinessException("用户名或密码错误");
        }
        if (member.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系客服");
        }
        loginGuard.clearFailures("member", username);
        return issueToken(member.getId(), member.getUsername());
    }

    public MemberVO info() {
        UmsMember member = memberMapper.selectById(UserContext.getUserId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return MemberVO.of(member);
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            stringRedisTemplate.delete(RedisKeyConstant.MEMBER_TOKEN + token.substring(7));
        }
    }

    // ==================== 个人资料 ====================

    /**
     * 修改个人资料（昵称/手机/头像），只更新非 null 字段
     */
    public void updateProfile(Long memberId, ProfileUpdateParam param) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        UmsMember update = new UmsMember();
        update.setId(memberId);
        if (param.getNickname() != null && !param.getNickname().isBlank()) {
            update.setNickname(param.getNickname().trim());
        }
        if (param.getPhone() != null && !param.getPhone().isBlank()) {
            // 手机号唯一检查
            long count = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                    .eq(UmsMember::getPhone, param.getPhone().trim())
                    .ne(UmsMember::getId, memberId));
            if (count > 0) {
                throw new BusinessException("该手机号已被其他账号绑定");
            }
            update.setPhone(param.getPhone().trim());
        }
        if (param.getIcon() != null) {
            update.setIcon(param.getIcon());
        }
        memberMapper.updateById(update);
    }

    // ==================== 忘记密码 ====================

    /**
     * 发送重置密码验证码（演示环境：不实际发短信，返回验证码供测试）
     */
    public String sendResetCode(String username) {
        UmsMember member = memberMapper.selectOne(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, username));
        if (member == null) {
            throw new BusinessException("用户名不存在");
        }
        if (member.getPhone() == null || member.getPhone().isBlank()) {
            throw new BusinessException("该账号未绑定手机号，无法找回密码，请联系客服");
        }
        // 生成 6 位数字验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        stringRedisTemplate.opsForValue().set(RESET_CODE_PREFIX + username, code,
                Duration.ofMinutes(10));
        // 演示环境返回验证码（生产环境应发短信）
        return code;
    }

    /**
     * 验证码重置密码
     */
    public void resetPassword(String username, String code, String newPassword) {
        String redisKey = RESET_CODE_PREFIX + username;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (!cached.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        if (!STRONG_PASSWORD.matcher(newPassword).matches()) {
            throw new BusinessException("密码需 8-32 位且包含字母和数字");
        }
        UmsMember member = memberMapper.selectOne(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, username));
        if (member == null) {
            throw new BusinessException("用户名不存在");
        }
        UmsMember update = new UmsMember();
        update.setId(member.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        memberMapper.updateById(update);
        // 重置成功后删除验证码 + 吊销旧 token
        stringRedisTemplate.delete(redisKey);
        String prefix = RedisKeyConstant.MEMBER_TOKEN;
        var keys = stringRedisTemplate.keys(prefix + "*");
        if (keys != null) {
            for (String k : keys) {
                String v = stringRedisTemplate.opsForValue().get(k);
                if (username.equals(v)) {
                    stringRedisTemplate.delete(k);
                }
            }
        }
    }

    // ==================== 后台会员管理 ====================

    /**
     * 后台会员分页（含订单数/累计消费，不返回密码）
     */
    public Page<Map<String, Object>> adminPage(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Page<UmsMember> page = memberMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsMember>()
                        .eq(status != null, UmsMember::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(UmsMember::getUsername, keyword.trim())
                                .or().like(UmsMember::getNickname, keyword.trim())
                                .or().like(UmsMember::getPhone, keyword.trim()))
                        .orderByDesc(UmsMember::getId));
        List<Long> memberIds = page.getRecords().stream().map(UmsMember::getId).toList();
        Map<Long, long[]> stats = orderStats(memberIds);
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(m -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("username", m.getUsername());
            row.put("nickname", m.getNickname());
            row.put("phone", m.getPhone());
            row.put("status", m.getStatus());
            row.put("createTime", m.getCreateTime());
            long[] st = stats.getOrDefault(m.getId(), new long[2]);
            row.put("orderCount", st[0]);
            row.put("totalAmount", st[1]);
            return row;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 后台封禁/解封会员
     */
    public void updateStatus(Long memberId, Integer status) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        UmsMember update = new UmsMember();
        update.setId(memberId);
        update.setStatus(status);
        memberMapper.updateById(update);
        // 封禁即吊销登录态：删除该会员全部 token
        if (status != null && status == 0) {
            String prefix = RedisKeyConstant.MEMBER_TOKEN;
            var keys = stringRedisTemplate.keys(prefix + "*");
            // token 的 value 为用户名，按用户名匹配删除
            if (keys != null) {
                for (String k : keys) {
                    String v = stringRedisTemplate.opsForValue().get(k);
                    if (member.getUsername().equals(v)) {
                        stringRedisTemplate.delete(k);
                    }
                }
            }
        }
    }

    /**
     * 批量统计会员已支付订单数与消费金额（status 1/2/3）
     */
    private Map<Long, long[]> orderStats(List<Long> memberIds) {
        Map<Long, long[]> result = new HashMap<>();
        if (memberIds.isEmpty()) {
            return result;
        }
        List<OmsOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .in(OmsOrder::getMemberId, memberIds)
                .in(OmsOrder::getStatus, List.of(1, 2, 3)));
        for (OmsOrder o : orders) {
            long[] st = result.computeIfAbsent(o.getMemberId(), k -> new long[2]);
            st[0]++;
            st[1] += o.getPayAmount() == null ? BigDecimal.ZERO.longValue() : o.getPayAmount().longValue();
        }
        return result;
    }

    private TokenVO issueToken(Long id, String username) {
        String token = jwtUtil.generate(id, "member", username);
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.MEMBER_TOKEN + token, username,
                Duration.ofSeconds(RedisKeyConstant.TOKEN_EXPIRE_SECONDS));
        return new TokenVO(token, id, username, "member");
    }
}
