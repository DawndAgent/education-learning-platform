package com.xxedu.learning.modules.category.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.category.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类")
@RestController
@RequestMapping(ApiConstants.PUBLIC_CATEGORIES)
@RequiredArgsConstructor
public class CategoryPublicController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树")
    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeVO>> tree() {
        return ApiResponse.ok(categoryService.publicTree());
    }

    @Operation(summary = "分类详情")
    @GetMapping("/{id}")
    public ApiResponse<CategoryVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.publicDetail(id));
    }
}
