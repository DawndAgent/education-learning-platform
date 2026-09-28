package com.xxedu.learning.modules.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.category.enums.CategoryStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    private Long parentId;
    private String name;
    private String code;
    private String iconUrl;
    private String description;
    private Integer sort;
    private CategoryStatus status;
}
