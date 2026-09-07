package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.component.OrderIdempotentHelper;
import com.cloude.shop.service.dto.OrderCreateParam;
import com.cloude.shop.service.dto.OrderDetailVO;
import com.cloude.shop.service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 */
@Tag(name = "OrderController", description = "订单")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderIdempotentHelper idempotentHelper;

    @Operation(summary = "创建订单（需携带 Idempotency-Key 请求头防重复提交，30分钟未支付自动关单）")
    @OpLog("ORDER_CREATE")
    @PostMapping("/create")
    public CommonResult<OrderDetailVO> create(@Valid @RequestBody OrderCreateParam param,
                                              @RequestHeader("Idempotency-Key") String idempotentKey) {
        Long memberId = UserContext.getUserId();
        idempotentHelper.tryAcquire(memberId, idempotentKey);
        try {
            OrderDetailVO vo = orderService.createOrder(memberId, param);
            idempotentHelper.complete(memberId, idempotentKey, vo.getOrderNo());
            return CommonResult.success(vo);
        } catch (Exception e) {
            // 下单失败释放幂等占位，允许客户端重试
            idempotentHelper.release(memberId, idempotentKey);
            throw e;
        }
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public CommonResult<OrderDetailVO> detail(@PathVariable String orderNo) {
        return CommonResult.success(orderService.detail(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping("/page")
    public CommonResult<Page<OrderDetailVO>> page(@RequestParam(required = false) Integer status,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(orderService.page(UserContext.getUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "取消订单（仅待付款）")
    @OpLog("ORDER_CANCEL")
    @PostMapping("/cancel/{orderNo}")
    public CommonResult<Void> cancel(@PathVariable String orderNo) {
        orderService.cancelOrder(UserContext.getUserId(), orderNo);
        return CommonResult.success();
    }

    @Operation(summary = "确认收货（已发货订单）")
    @OpLog("ORDER_CONFIRM")
    @PostMapping("/confirm/{orderId}")
    public CommonResult<Void> confirm(@PathVariable Long orderId) {
        orderService.confirmReceive(UserContext.getUserId(), orderId);
        return CommonResult.success();
    }
}
