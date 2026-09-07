package com.cloude.shop.admin.controller;

import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.mapper.entity.PmsCategory;
import com.cloude.shop.service.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台分类管理
 */
@Tag(name = "AdminCategoryController", description = "分类管理")
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表")
    @RequirePermission(PermissionCode.CATEGORY_LIST)
    @GetMapping("/list")
    public CommonResult<List<PmsCategory>> list() {
        return CommonResult.success(categoryService.adminList());
    }

    @Operation(summary = "创建分类")
    @RequirePermission(PermissionCode.CATEGORY_CREATE)
    @PostMapping("/create")
    public CommonResult<Void> create(@RequestBody PmsCategory category) {
        categoryService.create(category);
        return CommonResult.success();
    }

    @Operation(summary = "更新分类")
    @RequirePermission(PermissionCode.CATEGORY_UPDATE)
    @PostMapping("/update/{id}")
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody PmsCategory category) {
        category.setId(id);
        categoryService.update(category);
        return CommonResult.success();
    }

    @Operation(summary = "删除分类")
    @RequirePermission(PermissionCode.CATEGORY_DELETE)
    @PostMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return CommonResult.success();
    }
}
