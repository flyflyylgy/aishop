package com.cloude.shop.portal.controller;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.mapper.entity.UmsMemberAddress;
import com.cloude.shop.service.dto.AddressParam;
import com.cloude.shop.service.service.MemberAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址接口（需登录）
 */
@Tag(name = "AddressController", description = "收货地址簿")
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {

    private final MemberAddressService addressService;

    @Operation(summary = "地址列表（默认地址排最前）")
    @GetMapping("/list")
    public CommonResult<List<UmsMemberAddress>> list() {
        return CommonResult.success(addressService.list(UserContext.getUserId()));
    }

    @Operation(summary = "新增地址（每人上限 20 条，首条自动设为默认）")
    @OpLog("ADDRESS_CREATE")
    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AddressParam param) {
        return CommonResult.success(addressService.add(UserContext.getUserId(), param));
    }

    @Operation(summary = "编辑地址")
    @OpLog("ADDRESS_UPDATE")
    @PostMapping("/update/{id}")
    public CommonResult<Void> update(@PathVariable Long id, @Valid @RequestBody AddressParam param) {
        addressService.update(UserContext.getUserId(), id, param);
        return CommonResult.success();
    }

    @Operation(summary = "删除地址（删默认后自动提升最近一条）")
    @OpLog("ADDRESS_DELETE")
    @PostMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return CommonResult.success();
    }

    @Operation(summary = "设为默认地址")
    @OpLog("ADDRESS_DEFAULT")
    @PostMapping("/default/{id}")
    public CommonResult<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.getUserId(), id);
        return CommonResult.success();
    }
}
