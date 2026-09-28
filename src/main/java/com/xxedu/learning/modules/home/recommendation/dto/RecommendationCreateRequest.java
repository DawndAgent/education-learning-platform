package com.xxedu.learning.modules.home.recommendation.dto;

import com.xxedu.learning.modules.home.enums.RecommendType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendationCreateRequest {

    @NotNull(message = "推荐类型不能为空")
    private RecommendType recommendType;

    @NotNull(message = "推荐目标不能为空")
    private Long targetId;

    private Integer sort;
}
