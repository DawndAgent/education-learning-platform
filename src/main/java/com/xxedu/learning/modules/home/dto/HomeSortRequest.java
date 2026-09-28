package com.xxedu.learning.modules.home.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HomeSortRequest {

    @NotEmpty(message = "排序项不能为空")
    @Size(max = 100, message = "单次最多排序 100 条")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "ID 不能为空")
        private Long id;

        @NotNull(message = "排序不能为空")
        private Integer sort;
    }
}
