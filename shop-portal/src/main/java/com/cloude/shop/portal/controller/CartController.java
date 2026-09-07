package com.cloude.shop.portal.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.dto.CartVO;
import com.cloude.shop.service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 */
@Tag(name = "CartController", description = "购物车")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping("/list")
    public CommonResult<List<CartVO>> list() {
        return CommonResult.success(cartService.list(UserContext.getUserId()));
    }

    @Operation(summary = "加入购物车")
    @OpLog("CART_ADD")
    @PostMapping("/add")
    public CommonResult<Void> add(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = body.containsKey("quantity")
                ? Integer.valueOf(String.valueOf(body.get("quantity"))) : 1;
        cartService.add(UserContext.getUserId(), productId, quantity);
        return CommonResult.success();
    }

    @Operation(summary = "修改数量")
    @OpLog("CART_UPDATE_QUANTITY")
    @PostMapping("/quantity/{itemId}/{quantity}")
    public CommonResult<Void> updateQuantity(@PathVariable Long itemId, @PathVariable Integer quantity) {
        cartService.updateQuantity(UserContext.getUserId(), itemId, quantity);
        return CommonResult.success();
    }

    @Operation(summary = "勾选/取消勾选")
    @PostMapping("/selected/{itemId}/{selected}")
    public CommonResult<Void> updateSelected(@PathVariable Long itemId, @PathVariable Integer selected) {
        cartService.updateSelected(UserContext.getUserId(), itemId, selected);
        return CommonResult.success();
    }

    @Operation(summary = "删除购物车条目")
    @OpLog("CART_DELETE")
    @PostMapping("/delete/{itemId}")
    public CommonResult<Void> delete(@PathVariable Long itemId) {
        cartService.delete(UserContext.getUserId(), itemId);
        return CommonResult.success();
    }

    @Operation(summary = "清空购物车")
    @OpLog("CART_CLEAR")
    @PostMapping("/clear")
    public CommonResult<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return CommonResult.success();
    }
}
