package com.xxedu.learning.modules.question.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionPdfParseResultVO {

    private String fileName;
    private Integer pageCount;
    private Integer questionCount;
    private List<QuestionPdfParsedItemVO> questions = new ArrayList<>();
}
