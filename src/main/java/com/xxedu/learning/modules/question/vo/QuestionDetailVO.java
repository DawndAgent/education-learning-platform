package com.xxedu.learning.modules.question.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.question.enums.QuestionDifficulty;
import com.xxedu.learning.modules.question.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QuestionDetailVO {

    private Long contentId;
    private String title;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private QuestionType questionType;
    private String questionText;
    private String questionImageUrl;
    private String answerText;
    private String answerImageUrl;
    private String analysisText;
    private String analysisImageUrl;
    private QuestionDifficulty difficulty;
}
