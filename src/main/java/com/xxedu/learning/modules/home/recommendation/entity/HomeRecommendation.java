package com.xxedu.learning.modules.home.recommendation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.RecommendType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_recommendation")
public class HomeRecommendation extends BaseEntity {

    private String title;
    private RecommendType recommendType;
    private Long targetId;
    private Integer sort;
    private HomeItemStatus status;
}
