package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.entity.OmsOrderItem;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsReview;
import com.cloude.shop.mapper.entity.UmsMember;
import com.cloude.shop.mapper.mapper.OmsOrderItemMapper;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.mapper.mapper.PmsReviewMapper;
import com.cloude.shop.mapper.mapper.UmsMemberMapper;
import com.cloude.shop.service.dto.ReviewCreateParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品评价服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PmsReviewMapper reviewMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final UmsMemberMapper memberMapper;
    private final PmsProductMapper productMapper;

    /**
     * 发表评价（仅已完成订单可评，每商品每订单只能评一次）
     */
    public void create(ReviewCreateParam param) {
        Long memberId = UserContext.getUserId();
        OmsOrder order = orderMapper.selectById(param.getOrderId());
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new BusinessException("仅已完成订单可评价");
        }
        // 校验商品是否属于该订单
        List<OmsOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderId, param.getOrderId()));
        boolean productInOrder = items.stream().anyMatch(i -> i.getProductId().equals(param.getProductId()));
        if (!productInOrder) {
            throw new BusinessException("该商品不在订单中");
        }
        // 防重复评价
        long exist = reviewMapper.selectCount(new LambdaQueryWrapper<PmsReview>()
                .eq(PmsReview::getOrderId, param.getOrderId())
                .eq(PmsReview::getProductId, param.getProductId())
                .eq(PmsReview::getMemberId, memberId));
        if (exist > 0) {
            throw new BusinessException("您已评价过该商品");
        }

        UmsMember member = memberMapper.selectById(memberId);
        PmsReview review = new PmsReview();
        review.setProductId(param.getProductId());
        review.setMemberId(memberId);
        review.setMemberName(member != null ? member.getNickname() : "匿名用户");
        review.setOrderId(param.getOrderId());
        review.setOrderNo(order.getOrderNo());
        review.setRating(param.getRating());
        review.setContent(param.getContent());
        review.setPics(param.getPics());
        reviewMapper.insert(review);
    }

    /**
     * 商品评价分页（前台公开）
     */
    public Page<PmsReview> pageByProduct(Long productId, Integer pageNum, Integer pageSize) {
        return reviewMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PmsReview>()
                        .eq(PmsReview::getProductId, productId)
                        .orderByDesc(PmsReview::getId));
    }

    /**
     * 后台评价分页
     */
    public Page<PmsReview> adminPage(Long productId, Integer pageNum, Integer pageSize) {
        return reviewMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PmsReview>()
                        .eq(productId != null, PmsReview::getProductId, productId)
                        .orderByDesc(PmsReview::getId));
    }

    /**
     * 统计商品评价数和平均分
     */
    public Map<String, Object> stats(Long productId) {
        List<PmsReview> all = reviewMapper.selectList(new LambdaQueryWrapper<PmsReview>()
                .eq(PmsReview::getProductId, productId));
        long count = all.size();
        double avg = all.stream().mapToInt(PmsReview::getRating).average().orElse(0.0);
        return Map.of("count", count, "avgRating", Math.round(avg * 10) / 10.0);
    }

    /**
     * 我的评价列表（附带商品名称）
     */
    public Page<Map<String, Object>> myReviews(Integer pageNum, Integer pageSize) {
        Page<PmsReview> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PmsReview>()
                        .eq(PmsReview::getMemberId, UserContext.getUserId())
                        .orderByDesc(PmsReview::getId));
        List<Long> productIds = page.getRecords().stream()
                .map(PmsReview::getProductId).distinct().toList();
        Map<Long, String> nameMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(PmsProduct::getId, PmsProduct::getName, (a, b) -> a));
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("productId", r.getProductId());
            m.put("productName", nameMap.getOrDefault(r.getProductId(), "已下架商品"));
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("pics", r.getPics());
            m.put("orderNo", r.getOrderNo());
            m.put("createTime", r.getCreateTime());
            return m;
        }).toList());
        return result;
    }
}
