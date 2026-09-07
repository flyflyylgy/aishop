package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.SmsCoupon;
import com.cloude.shop.mapper.entity.SmsCouponHistory;
import com.cloude.shop.mapper.mapper.SmsCouponHistoryMapper;
import com.cloude.shop.mapper.mapper.SmsCouponMapper;
import com.cloude.shop.service.dto.CouponVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券服务 —— 领券 / 校验 / 折扣计算 / 核销 / 关单回退
 * <p>
 * 券类型：1-满减券 2-折扣券 3-现金券(无门槛)
 * 防超发：领取数量 CAS 自增（received_count &lt; total_count）；核销 CAS 0-&gt;1 防重复使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final SmsCouponMapper couponMapper;
    private final SmsCouponHistoryMapper historyMapper;

    private static final BigDecimal MIN_PAY = new BigDecimal("0.01");

    // ==================== 后台管理 ====================

    public Page<SmsCoupon> adminPage(Integer status, Integer type, String keyword, Integer pageNum, Integer pageSize) {
        return couponMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsCoupon>()
                        .eq(status != null, SmsCoupon::getStatus, status)
                        .eq(type != null, SmsCoupon::getType, type)
                        .like(keyword != null && !keyword.isBlank(), SmsCoupon::getName, keyword.trim())
                        .orderByDesc(SmsCoupon::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SmsCoupon coupon) {
        validateCoupon(coupon);
        coupon.setId(null);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        if (coupon.getStatus() == null) {
            coupon.setStatus(1);
        }
        couponMapper.insert(coupon);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SmsCoupon coupon) {
        if (coupon.getId() == null) {
            throw new BusinessException("优惠券ID不能为空");
        }
        SmsCoupon exist = couponMapper.selectById(coupon.getId());
        if (exist == null) {
            throw new BusinessException("优惠券不存在");
        }
        validateCoupon(coupon);
        // 领取/使用数量由系统维护，编辑接口不允许覆盖
        coupon.setReceivedCount(null);
        coupon.setUsedCount(null);
        coupon.setDeleteFlag(null);
        couponMapper.updateById(coupon);
    }

    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值非法");
        }
        SmsCoupon update = new SmsCoupon();
        update.setId(id);
        update.setStatus(status);
        couponMapper.updateById(update);
    }

    private void validateCoupon(SmsCoupon c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new BusinessException("券名称不能为空");
        }
        Integer type = c.getType();
        if (type == null || type < 1 || type > 3) {
            throw new BusinessException("优惠券类型非法(1满减/2折扣/3现金)");
        }
        if (type == 1 || type == 3) {
            if (c.getFaceValue() == null || c.getFaceValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("面额必须大于0");
            }
            c.setDiscount(null);
        } else {
            if (c.getDiscount() == null || c.getDiscount() < 1 || c.getDiscount() > 99) {
                throw new BusinessException("折扣取值范围 1-99(如85表示8.5折)");
            }
            c.setFaceValue(null);
        }
        if (c.getMinPoint() == null) {
            c.setMinPoint(BigDecimal.ZERO);
        }
        if (c.getMinPoint().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("使用门槛不能为负");
        }
        if (type == 1 && c.getMinPoint().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("满减券需设置使用门槛");
        }
        if (c.getTotalCount() == null || c.getTotalCount() <= 0) {
            throw new BusinessException("发行总量必须大于0");
        }
        if (c.getPerLimit() == null || c.getPerLimit() <= 0) {
            c.setPerLimit(1);
        }
        if (c.getStartTime() == null || c.getEndTime() == null || c.getEndTime().isBefore(c.getStartTime())) {
            throw new BusinessException("领取时间区间非法");
        }
        if (c.getValidDays() == null || c.getValidDays() <= 0) {
            c.setValidDays(30);
        }
    }

    // ==================== 前台会员 ====================

    /**
     * 领券中心：上架且在领取期内、未领完的券，附带当前会员是否已达限领
     */
    public List<CouponVO> center(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<SmsCoupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<SmsCoupon>()
                .eq(SmsCoupon::getStatus, 1)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)
                .apply("received_count < total_count")
                .orderByDesc(SmsCoupon::getId));
        return coupons.stream().map(c -> {
            CouponVO vo = new CouponVO();
            copyCoupon(c, vo);
            vo.setHistoryId(null);
            vo.setRemaining(c.getTotalCount() - c.getReceivedCount());
            Long owned = historyMapper.selectCount(new LambdaQueryWrapper<SmsCouponHistory>()
                    .eq(SmsCouponHistory::getCouponId, c.getId())
                    .eq(SmsCouponHistory::getMemberId, memberId));
            vo.setClaimed(owned >= c.getPerLimit());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 领取优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long memberId, Long couponId) {
        SmsCoupon c = couponMapper.selectById(couponId);
        if (c == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (c.getStatus() == null || c.getStatus() != 1) {
            throw new BusinessException("优惠券已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(c.getStartTime()) || now.isAfter(c.getEndTime())) {
            throw new BusinessException("不在优惠券领取时间内");
        }
        Long owned = historyMapper.selectCount(new LambdaQueryWrapper<SmsCouponHistory>()
                .eq(SmsCouponHistory::getCouponId, couponId)
                .eq(SmsCouponHistory::getMemberId, memberId));
        if (owned >= c.getPerLimit()) {
            throw new BusinessException("您已领取过该优惠券");
        }
        // CAS 自增领取数，affected=0 即已抢光
        int affected = couponMapper.casIncreaseReceived(couponId);
        if (affected == 0) {
            throw new BusinessException("优惠券已被领完");
        }
        SmsCouponHistory h = new SmsCouponHistory();
        h.setCouponId(couponId);
        h.setMemberId(memberId);
        h.setStatus(0);
        h.setReceiveTime(now);
        h.setExpireTime(now.plusDays(c.getValidDays() == null ? 30 : c.getValidDays()));
        historyMapper.insert(h);
        log.info("会员领取优惠券, memberId={}, couponId={}", memberId, couponId);
    }

    /**
     * 我的优惠券分页（status: 0未使用 1已使用 2已过期；null 全部）
     */
    public Page<CouponVO> myCoupons(Long memberId, Integer status, Integer pageNum, Integer pageSize) {
        Page<SmsCouponHistory> page = historyMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsCouponHistory>()
                        .eq(SmsCouponHistory::getMemberId, memberId)
                        .orderByDesc(SmsCouponHistory::getId));
        List<SmsCouponHistory> records = page.getRecords();
        Map<Long, SmsCoupon> couponMap = records.isEmpty() ? Map.of()
                : couponMapper.selectBatchIds(records.stream().map(SmsCouponHistory::getCouponId).toList())
                .stream().collect(Collectors.toMap(SmsCoupon::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<CouponVO> voList = new ArrayList<>();
        for (SmsCouponHistory h : records) {
            SmsCoupon c = couponMap.get(h.getCouponId());
            if (c == null) {
                continue;
            }
            int effectiveStatus = h.getStatus();
            if (effectiveStatus == 0 && h.getExpireTime() != null && h.getExpireTime().isBefore(now)) {
                effectiveStatus = 2;
            }
            if (status != null && effectiveStatus != status) {
                continue;
            }
            CouponVO vo = new CouponVO();
            copyCoupon(c, vo);
            vo.setHistoryId(h.getId());
            vo.setHistoryStatus(effectiveStatus);
            vo.setReceiveTime(h.getReceiveTime());
            vo.setExpireTime(h.getExpireTime());
            vo.setUseTime(h.getUseTime());
            vo.setOrderNo(h.getOrderNo());
            voList.add(vo);
        }
        Page<CouponVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    // ==================== 下单用券 ====================

    /**
     * 校验并计算优惠金额（下单事务内调用，不落库）
     */
    public CouponUse validateAndCalc(Long memberId, Long historyId, BigDecimal goodsAmount) {
        SmsCouponHistory h = historyMapper.selectById(historyId);
        if (h == null || !h.getMemberId().equals(memberId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (h.getStatus() != null && h.getStatus() == 1) {
            throw new BusinessException("优惠券已使用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (h.getExpireTime() != null && h.getExpireTime().isBefore(now)) {
            throw new BusinessException("优惠券已过期");
        }
        SmsCoupon c = couponMapper.selectById(h.getCouponId());
        if (c == null) {
            throw new BusinessException("优惠券不存在");
        }
        BigDecimal discount = computeDiscount(c, goodsAmount);
        CouponUse use = new CouponUse();
        use.setHistoryId(h.getId());
        use.setCouponId(c.getId());
        use.setCouponName(c.getName());
        use.setDiscount(discount);
        return use;
    }

    /**
     * 核销优惠券（订单落库后调用，CAS 0->1 防重复）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long historyId, String orderNo) {
        int affected = historyMapper.update(null, new LambdaUpdateWrapper<SmsCouponHistory>()
                .eq(SmsCouponHistory::getId, historyId)
                .eq(SmsCouponHistory::getStatus, 0)
                .set(SmsCouponHistory::getStatus, 1)
                .set(SmsCouponHistory::getOrderNo, orderNo)
                .set(SmsCouponHistory::getUseTime, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException("优惠券已被使用或不可用");
        }
        SmsCouponHistory h = historyMapper.selectById(historyId);
        couponMapper.increaseUsed(h.getCouponId());
    }

    /**
     * 关单回退优惠券（超时/取消待付款订单时调用）：恢复为未使用，已过期则置为过期
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseByOrder(String orderNo) {
        SmsCouponHistory h = historyMapper.selectOne(new LambdaQueryWrapper<SmsCouponHistory>()
                .eq(SmsCouponHistory::getOrderNo, orderNo));
        if (h == null || h.getStatus() == null || h.getStatus() != 1) {
            return;
        }
        boolean expired = h.getExpireTime() != null && h.getExpireTime().isBefore(LocalDateTime.now());
        int affected = historyMapper.update(null, new LambdaUpdateWrapper<SmsCouponHistory>()
                .eq(SmsCouponHistory::getId, h.getId())
                .eq(SmsCouponHistory::getStatus, 1)
                .set(SmsCouponHistory::getStatus, expired ? 2 : 0)
                .set(SmsCouponHistory::getOrderNo, null)
                .set(SmsCouponHistory::getUseTime, null));
        if (affected > 0) {
            couponMapper.decreaseUsed(h.getCouponId());
            log.info("关单回退优惠券, orderNo={}, couponHistoryId={}, expired={}", orderNo, h.getId(), expired);
        }
    }

    /**
     * 按券类型计算折扣金额（校验门槛），并保证支付金额不低于 0.01
     */
    private BigDecimal computeDiscount(SmsCoupon c, BigDecimal goodsAmount) {
        BigDecimal minPoint = c.getMinPoint() == null ? BigDecimal.ZERO : c.getMinPoint();
        if (goodsAmount.compareTo(minPoint) < 0) {
            throw new BusinessException("未达到优惠券使用门槛(满 " + minPoint + " 元可用)");
        }
        BigDecimal discount;
        int type = c.getType() == null ? 0 : c.getType();
        if (type == 1 || type == 3) {
            discount = c.getFaceValue();
        } else if (type == 2) {
            // discount=85 表示 8.5 折，优惠 = 金额 × (100-85)/100
            discount = goodsAmount.multiply(BigDecimal.valueOf(100L - c.getDiscount()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            throw new BusinessException("优惠券类型异常");
        }
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("优惠券金额异常");
        }
        // 优惠不超过 商品金额 - 0.01，保证应付不为 0/负
        BigDecimal maxDiscount = goodsAmount.subtract(MIN_PAY).max(BigDecimal.ZERO);
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount;
    }

    private void copyCoupon(SmsCoupon c, CouponVO vo) {
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setFaceValue(c.getFaceValue());
        vo.setDiscount(c.getDiscount());
        vo.setMinPoint(c.getMinPoint());
        vo.setTotalCount(c.getTotalCount());
        vo.setReceivedCount(c.getReceivedCount());
        vo.setUsedCount(c.getUsedCount());
        vo.setPerLimit(c.getPerLimit());
        vo.setStartTime(c.getStartTime());
        vo.setEndTime(c.getEndTime());
        vo.setValidDays(c.getValidDays());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
    }

    /** 用券计算结果 */
    @Data
    public static class CouponUse {
        private Long historyId;
        private Long couponId;
        private String couponName;
        private BigDecimal discount;
    }
}
