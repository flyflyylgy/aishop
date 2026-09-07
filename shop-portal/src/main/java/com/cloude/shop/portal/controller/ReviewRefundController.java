package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.mapper.entity.PmsReview;
import com.cloude.shop.service.dto.RefundApplyParam;
import com.cloude.shop.service.dto.ReviewCreateParam;
import com.cloude.shop.service.service.RefundService;
import com.cloude.shop.service.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价 + 退款接口（前台会员）
 */
@Tag(name = "ReviewRefundController", description = "评价与退款")
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewRefundController {

    private final ReviewService reviewService;
    private final RefundService refundService;

    // ==================== 评价 ====================

    @Operation(summary = "发表评价")
    @OpLog("MEMBER_REVIEW")
    @PostMapping
    public CommonResult<Void> createReview(@Valid @RequestBody ReviewCreateParam param) {
        reviewService.create(param);
        return CommonResult.success();
    }

    @Operation(summary = "商品评价分页（公开）")
    @GetMapping("/product/{productId}")
    public CommonResult<Page<PmsReview>> productReviews(@PathVariable Long productId,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(reviewService.pageByProduct(productId, pageNum, pageSize));
    }

    @Operation(summary = "商品评价统计（公开）")
    @GetMapping("/product/{productId}/stats")
    public CommonResult<Map<String, Object>> reviewStats(@PathVariable Long productId) {
        return CommonResult.success(reviewService.stats(productId));
    }

    // ==================== 退款 ====================

    @Operation(summary = "申请退款")
    @OpLog("MEMBER_REFUND_APPLY")
    @PostMapping("/refund/apply")
    public CommonResult<Void> applyRefund(@Valid @RequestBody RefundApplyParam param) {
        refundService.apply(param);
        return CommonResult.success();
    }

    @Operation(summary = "我的退款列表")
    @GetMapping("/refund/my")
    public CommonResult<Page<com.cloude.shop.mapper.entity.OmsRefund>> myRefunds(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(refundService.myRefunds(pageNum, pageSize));
    }
}
