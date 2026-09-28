package com.xxedu.learning.modules.video.dto;

import com.xxedu.learning.modules.video.enums.VideoSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VideoUpdateRequest {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过128")
    private String title;

    @Size(max = 255, message = "封面地址长度不能超过255")
    private String coverUrl;

    @Size(max = 512, message = "摘要长度不能超过512")
    private String summary;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;

    @NotNull(message = "视频来源不能为空")
    private VideoSourceType sourceType;

    @Size(max = 512, message = "视频地址长度不能超过512")
    private String videoUrl;

    @Size(max = 512, message = "二维码地址长度不能超过512")
    private String qrCodeUrl;

    @Min(value = 0, message = "时长不能小于0")
    private Integer duration;
}
