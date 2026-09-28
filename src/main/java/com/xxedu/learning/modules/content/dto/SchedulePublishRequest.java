package com.xxedu.learning.modules.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchedulePublishRequest {

    @NotBlank(message = "发布时间不能为空")
    private String publishTime;
}
