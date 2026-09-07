package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.mapper.entity.SmsCoupon;
import com.cloude.shop.service.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台优惠券管理
 */
@Tag(name = "AdminCouponController", description = "优惠券管理")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @Operation(summary = "优惠券分页列表")
    @RequirePermission(PermissionCode.COUPON_LIST)
    @GetMapping("/page")
    public CommonResult<Page<SmsCoupon>> page(@RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) Integer type,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(couponService.adminPage(status, type, keyword, pageNum, pageSize));
    }

    @Operation(summary = "创建优惠券")
    @OpLog("COUPON_CREATE")
    @RequirePermission(PermissionCode.COUPON_CREATE)
    @PostMapping("/create")
    public CommonResult<Void> create(@RequestBody SmsCoupon coupon) {
        couponService.create(coupon);
        return CommonResult.success();
    }

    @Operation(summary = "更新优惠券")
    @OpLog("COUPON_UPDATE")
    @RequirePermission(PermissionCode.COUPON_UPDATE)
    @PostMapping("/update/{id}")
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody SmsCoupon coupon) {
        coupon.setId(id);
        couponService.update(coupon);
        return CommonResult.success();
    }

    @Operation(summary = "上架/下架")
    @OpLog("COUPON_STATUS")
    @RequirePermission(PermissionCode.COUPON_STATUS)
    @PostMapping("/status/{id}/{status}")
    public CommonResult<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        couponService.changeStatus(id, status);
        return CommonResult.success();
    }
}
