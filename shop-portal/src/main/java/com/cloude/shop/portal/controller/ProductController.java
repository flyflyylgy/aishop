package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.service.dto.CartVO;
import com.cloude.shop.service.service.CategoryService;
import com.cloude.shop.service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "商品详情")
    @GetMapping("/{id:\\d+}")
    public CommonResult<PmsProduct> detail(@PathVariable Long id) {
        return CommonResult.success(productService.detail(id));
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
