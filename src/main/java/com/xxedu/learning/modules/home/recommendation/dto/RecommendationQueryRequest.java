package com.xxedu.learning.modules.home.recommendation.dto;

import com.xxedu.learning.common.api.PageQuery;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.RecommendType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendationQueryRequest extends PageQuery {

    private RecommendType recommendType;
    private HomeItemStatus status;
}
