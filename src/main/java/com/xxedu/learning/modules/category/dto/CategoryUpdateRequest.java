package com.xxedu.learning.modules.category.dto;

import com.xxedu.learning.modules.category.enums.CategoryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateRequest {

    @NotNull(message = "父分类不能为空")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过64")
    private String name;

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "分类编码必须是大写字母、数字或下划线")
    private String code;

    @Size(max = 255, message = "图标地址长度不能超过255")
    private String iconUrl;

    @Size(max = 255, message = "分类说明长度不能超过255")
    private String description;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    private CategoryStatus status;
}
