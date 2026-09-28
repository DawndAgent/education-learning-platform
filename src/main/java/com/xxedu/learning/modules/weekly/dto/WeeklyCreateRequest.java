package com.xxedu.learning.modules.weekly.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WeeklyCreateRequest {

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

    @Size(max = 50, message = "周次标签长度不能超过50")
    private String weekLabel;

    @Size(max = 20000, message = "题目内容长度不能超过20000")
    private String questionText;

    @Size(max = 500, message = "题目图片地址长度不能超过500")
    private String questionImageUrl;

    @Size(max = 20000, message = "答案长度不能超过20000")
    private String answerText;

    @Size(max = 500, message = "答案图片地址长度不能超过500")
    private String answerImageUrl;

    @Size(max = 20000, message = "解析长度不能超过20000")
    private String analysisText;

    @Size(max = 500, message = "解析图片地址长度不能超过500")
    private String analysisImageUrl;
}
