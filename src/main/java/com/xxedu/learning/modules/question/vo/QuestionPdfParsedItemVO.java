package com.xxedu.learning.modules.question.vo;

import com.xxedu.learning.modules.question.enums.QuestionType;
import lombok.Data;

@Data
public class QuestionPdfParsedItemVO {

    private Integer index;
    private String label;
    private String title;
    private QuestionType questionType;
    private String questionText;
    private String answerText;
    private String analysisText;
    private Boolean suspicious;
    private String suspiciousReason;
}
