package com.xxedu.learning.modules.category.vo;

import com.xxedu.learning.modules.category.enums.CategoryStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryVO {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String iconUrl;
    private String description;
    private Integer sort;
    private CategoryStatus status;
}
