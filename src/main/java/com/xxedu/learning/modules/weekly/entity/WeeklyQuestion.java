package com.xxedu.learning.modules.weekly.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("weekly_question")
public class WeeklyQuestion extends BaseEntity {

    private Long contentId;
    private String weekLabel;
    private String questionText;
    private String questionImageUrl;
    private String answerText;
    private String answerImageUrl;
    private String analysisText;
    private String analysisImageUrl;
}
