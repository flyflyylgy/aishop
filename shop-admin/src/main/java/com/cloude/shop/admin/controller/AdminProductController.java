package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.mapper.entity.PmsCategory;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsSku;
import com.cloude.shop.service.dto.ProductQuery;
import com.cloude.shop.service.service.CategoryService;
import com.cloude.shop.service.service.ProductService;
import com.cloude.shop.service.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台商品管理
 */
@Tag(name = "AdminProductController", description = "商品管理")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SkuService skuService;

    @Operation(summary = "商品分页列表")
    @RequirePermission(PermissionCode.PRODUCT_LIST)
    @GetMapping("/page")
    public CommonResult<Page<PmsProduct>> page(ProductQuery query) {
        return CommonResult.success(productService.adminPage(query));
    }

    @Operation(summary = "商品详情")
    @RequirePermission(PermissionCode.PRODUCT_LIST)
    @GetMapping("/{id}")
    public CommonResult<PmsProduct> detail(@PathVariable Long id) {
        return CommonResult.success(productService.detailForAdmin(id));
    }

    @Operation(summary = "创建商品")
    @RequirePermission(PermissionCode.PRODUCT_CREATE)
    @PostMapping("/create")
    public CommonResult<Void> create(@Valid @RequestBody PmsProduct product) {
        productService.create(product);
        return CommonResult.success();
    }

    @Operation(summary = "更新商品")
    @RequirePermission(PermissionCode.PRODUCT_UPDATE)
    @PostMapping("/update/{id}")
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody PmsProduct product) {
        product.setId(id);
        productService.update(product);
        return CommonResult.success();
    }

    @Operation(summary = "上架/下架")
    @RequirePermission(PermissionCode.PRODUCT_STATUS)
    @PostMapping("/status/{id}/{status}")
    public CommonResult<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        productService.changeStatus(id, status);
        return CommonResult.success();
    }

    @Operation(summary = "删除商品")
    @RequirePermission(PermissionCode.PRODUCT_DELETE)
    @PostMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return CommonResult.success();
    }

    @Operation(summary = "分类平铺列表")
    @RequirePermission(PermissionCode.PRODUCT_LIST)
    @GetMapping("/categories")
    public CommonResult<List<PmsCategory>> categories() {
        return CommonResult.success(categoryService.adminList());
    }

    @Operation(summary = "查询商品 SKU 列表")
    @RequirePermission(PermissionCode.PRODUCT_LIST)
    @GetMapping("/skus/{productId}")
    public CommonResult<List<PmsSku>> listSkus(@PathVariable Long productId) {
        return CommonResult.success(skuService.listAllByProduct(productId));
    }

    @Operation(summary = "保存商品 SKU（批量覆盖）")
    @RequirePermission(PermissionCode.PRODUCT_UPDATE)
    @PostMapping("/skus/{productId}")
    public CommonResult<Void> saveSkus(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<PmsSku> skus = ((List<Map<String, Object>>) body.get("skus")).stream()
                .map(m -> {
                    PmsSku sku = new PmsSku();
                    if (m.get("id") != null) {
                        sku.setId(Long.valueOf(String.valueOf(m.get("id"))));
                    }
                    sku.setSkuCode(m.get("skuCode") != null ? String.valueOf(m.get("skuCode")) : null);
                    sku.setSpecValues(String.valueOf(m.get("specValues")));
                    sku.setPrice(new java.math.BigDecimal(String.valueOf(m.get("price"))));
                    sku.setAvailableStock(Integer.valueOf(String.valueOf(m.get("availableStock"))));
                    sku.setStatus(m.get("status") != null ? Integer.valueOf(String.valueOf(m.get("status"))) : 1);
                    return sku;
                }).toList();
        skuService.batchSave(productId, skus);
        return CommonResult.success();
    }
}
