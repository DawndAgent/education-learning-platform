package com.xxedu.learning.modules.home.banner.dto;

import com.xxedu.learning.modules.home.enums.HomeLinkType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Size(max = 500, message = "副标题长度不能超过500")
    private String subtitle;

    @NotBlank(message = "图片不能为空")
    @Size(max = 500, message = "图片地址长度不能超过500")
    private String imageUrl;

    @NotNull(message = "跳转类型不能为空")
    private HomeLinkType linkType;

    private Long linkId;

    @Size(max = 1000, message = "链接长度不能超过1000")
    private String linkUrl;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
