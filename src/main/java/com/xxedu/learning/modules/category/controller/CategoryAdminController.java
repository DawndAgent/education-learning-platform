package com.xxedu.learning.modules.category.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.category.dto.CategoryCreateRequest;
import com.xxedu.learning.modules.category.dto.CategoryUpdateRequest;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.category.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_CATEGORIES)
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    @Operation(summary = "管理端分类树")
    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeVO>> tree() {
        return ApiResponse.ok(categoryService.adminTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public ApiResponse<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.ok(categoryService.update(id, request));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok(null);
    }
}
