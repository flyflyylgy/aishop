package com.cloude.shop.portal.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.dto.PayNotifyParam;
import com.cloude.shop.service.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付接口
 */
@Tag(name = "PayController", description = "支付（模拟通道）")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @Operation(summary = "创建支付单（模拟统一下单）")
    @OpLog("PAY_CREATE")
    @PostMapping("/create/{orderNo}")
    public CommonResult<Map<String, Object>> createPay(@PathVariable String orderNo) {
        return CommonResult.success(payService.createPay(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "支付回调（模拟第三方异步通知，幂等）")
    @OpLog("PAY_NOTIFY")
    @PostMapping("/notify")
    public CommonResult<Void> notify(@RequestBody PayNotifyParam param) {
        payService.handleNotify(param);
        return CommonResult.success();
    }
}
