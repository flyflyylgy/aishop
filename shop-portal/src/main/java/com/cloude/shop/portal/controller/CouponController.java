package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.dto.CouponVO;
import com.cloude.shop.service.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台优惠券接口（会员）
 */
@Tag(name = "CouponController", description = "优惠券")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心（可领取的券）")
    @GetMapping("/center")
    public CommonResult<List<CouponVO>> center() {
        return CommonResult.success(couponService.center(UserContext.getUserId()));
    }

    @Operation(summary = "领取优惠券")
    @OpLog("COUPON_RECEIVE")
    @PostMapping("/receive/{couponId}")
    public CommonResult<Void> receive(@PathVariable Long couponId) {
        couponService.claim(UserContext.getUserId(), couponId);
        return CommonResult.success();
    }

    @Operation(summary = "我的优惠券（status:0未使用 1已使用 2已过期，不传=全部）")
    @GetMapping("/my")
    public CommonResult<Page<CouponVO>> my(@RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(couponService.myCoupons(UserContext.getUserId(), status, pageNum, pageSize));
    }
}
