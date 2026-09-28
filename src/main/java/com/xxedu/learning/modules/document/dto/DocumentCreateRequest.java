package com.xxedu.learning.modules.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentCreateRequest {

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

    @Size(max = 1000, message = "文件地址长度不能超过1000")
    private String fileUrl;

    @Size(max = 255, message = "文件名长度不能超过255")
    private String fileName;

    @Min(value = 0, message = "文件大小不能小于0")
    private Long fileSize;

    @Size(max = 100, message = "文件类型长度不能超过100")
    private String fileType;

    @Size(max = 1000, message = "下载地址长度不能超过1000")
    private String downloadUrl;

    @Size(max = 1000, message = "预览地址长度不能超过1000")
    private String previewUrl;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;
}
