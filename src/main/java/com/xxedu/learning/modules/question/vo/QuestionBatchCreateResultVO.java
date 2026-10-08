package com.xxedu.learning.modules.question.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionBatchCreateResultVO {

    private Integer createdCount;
    private List<Long> contentIds = new ArrayList<>();
}
