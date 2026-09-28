package com.xxedu.learning.modules.category.vo;

import com.xxedu.learning.modules.category.enums.CategoryStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryTreeVO {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String iconUrl;
    private String description;
    private Integer sort;
    private CategoryStatus status;

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private List<CategoryTreeVO> children = new ArrayList<>();

    public List<CategoryTreeVO> getChildren() {
        if (children == null) {
            return List.of();
        }
        return List.copyOf(children);
    }

    public void setChildren(List<CategoryTreeVO> children) {
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    public void addChild(CategoryTreeVO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
