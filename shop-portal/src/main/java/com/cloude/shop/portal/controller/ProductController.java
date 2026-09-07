package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsSku;
import com.cloude.shop.service.dto.CartVO;
import com.cloude.shop.service.service.CategoryService;
import com.cloude.shop.service.service.ProductService;
import com.cloude.shop.service.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品浏览接口（无需登录）
 */
@Tag(name = "ProductController", description = "商品浏览")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SkuService skuService;

    @Operation(summary = "商品详情")
    @GetMapping("/{id:\\d+}")
    public CommonResult<Map<String, Object>> detail(@PathVariable Long id) {
        PmsProduct product = productService.detail(id);
        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("subTitle", product.getSubTitle());
        result.put("mainImage", product.getMainImage());
        result.put("subImages", product.getSubImages());
        result.put("price", product.getPrice());
        result.put("originalPrice", product.getOriginalPrice());
        result.put("detailHtml", product.getDetailHtml());
        result.put("availableStock", product.getAvailableStock());
        result.put("sale", product.getSale());
        result.put("status", product.getStatus());
        result.put("categoryId", product.getCategoryId());
        result.put("hasSku", product.getHasSku());
        result.put("specNames", product.getSpecNames());
        // 多规格商品附带 SKU 列表
        if (Integer.valueOf(1).equals(product.getHasSku())) {
            List<PmsSku> skus = skuService.listByProduct(id);
            result.put("skus", skus);
        }
        return CommonResult.success(result);
    }

    @Operation(summary = "商品搜索分页")
    @GetMapping("/page")
    public CommonResult<Page<PmsProduct>> page(@RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(productService.portalPage(categoryId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "热销推荐（缓存 10 分钟）")
    @GetMapping("/hot")
    public CommonResult<List<PmsProduct>> hot(@RequestParam(defaultValue = "6") Integer limit) {
        return CommonResult.success(productService.hot(Math.min(limit, 20)));
    }

    @Operation(summary = "前台分类树")
    @GetMapping("/category/tree")
    public CommonResult<List<CategoryService.CategoryNode>> categoryTree() {
        return CommonResult.success(categoryService.tree());
    }
}
