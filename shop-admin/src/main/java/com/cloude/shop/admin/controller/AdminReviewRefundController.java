package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.mapper.entity.OmsRefund;
import com.cloude.shop.mapper.entity.PmsReview;
import com.cloude.shop.service.service.RefundService;
import com.cloude.shop.service.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台评价管理 + 退款审批
 */
@Tag(name = "AdminReviewRefundController", description = "评价管理与退款审批")
@RestController
@RequestMapping("/review-refund")
@RequiredArgsConstructor
public class AdminReviewRefundController {

    private final ReviewService reviewService;
    private final RefundService refundService;

    // ==================== 评价管理 ====================

    @Operation(summary = "评价分页")
    @RequirePermission(PermissionCode.REVIEW_LIST)
    @GetMapping("/review/page")
    public CommonResult<Page<PmsReview>> reviewPage(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(reviewService.adminPage(productId, pageNum, pageSize));
    }

    // ==================== 退款审批 ====================

    @Operation(summary = "退款单分页")
    @RequirePermission(PermissionCode.REFUND_HANDLE)
    @GetMapping("/refund/page")
    public CommonResult<Page<OmsRefund>> refundPage(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(refundService.adminPage(status, pageNum, pageSize));
    }

    @Operation(summary = "审批退款")
    @RequirePermission(PermissionCode.REFUND_HANDLE)
    @OpLog("REFUND_HANDLE")
    @PostMapping("/refund/handle/{id}")
    public CommonResult<Void> handleRefund(@PathVariable Long id, @RequestBody HandleParam param) {
        refundService.handle(id, param.approved, param.adminRemark);
        return CommonResult.success();
    }

    @Data
    public static class HandleParam {
        private Boolean approved;
        private String adminRemark;
    }
}
