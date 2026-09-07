package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.service.dto.OrderDetailVO;
import com.cloude.shop.service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台订单管理
 */
@Tag(name = "AdminOrderController", description = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页（状态 + 订单号/收货人/电话筛选）")
    @RequirePermission(PermissionCode.ORDER_LIST)
    @GetMapping("/page")
    public CommonResult<Page<OrderDetailVO>> page(@RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(orderService.adminPage(status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @RequirePermission(PermissionCode.ORDER_LIST)
    @GetMapping("/{id}")
    public CommonResult<OrderDetailVO> detail(@PathVariable Long id) {
        return CommonResult.success(orderService.adminDetail(id));
    }

    @Operation(summary = "订单发货（登记物流公司与运单号）")
    @OpLog("ORDER_SHIP")
    @RequirePermission(PermissionCode.ORDER_SHIP)
    @PostMapping("/ship/{orderId}")
    public CommonResult<Void> ship(@PathVariable Long orderId, @Valid @RequestBody ShipParam param) {
        orderService.ship(orderId, param.getExpressCompany(), param.getExpressNo());
        return CommonResult.success();
    }

    @Operation(summary = "后台订单备注")
    @OpLog("ORDER_REMARK")
    @RequirePermission(PermissionCode.ORDER_REMARK)
    @PostMapping("/remark/{orderId}")
    public CommonResult<Void> remark(@PathVariable Long orderId, @RequestBody RemarkParam param) {
        orderService.updateAdminRemark(orderId, param.getRemark());
        return CommonResult.success();
    }

    @Data
    public static class ShipParam {
        @NotBlank(message = "物流公司不能为空")
        private String expressCompany;
        @NotBlank(message = "运单号不能为空")
        private String expressNo;
    }

    @Data
    public static class RemarkParam {
        private String remark;
    }
}
