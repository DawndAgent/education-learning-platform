package com.xxedu.learning.modules.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.question.enums.QuestionDifficulty;
import com.xxedu.learning.modules.question.enums.QuestionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    private Long contentId;
    private QuestionType questionType;
    private String questionText;
    private String questionImageUrl;
    private String answerText;
    private String answerImageUrl;
    private String analysisText;
    private String analysisImageUrl;
    private QuestionDifficulty difficulty;
}
