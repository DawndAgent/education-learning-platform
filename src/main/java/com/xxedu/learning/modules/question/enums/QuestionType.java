package com.xxedu.learning.modules.question.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {

    SINGLE_CHOICE("SINGLE_CHOICE"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE"),
    FILL_BLANK("FILL_BLANK"),
    ANSWER("ANSWER"),
    PROOF("PROOF"),
    IMAGE_QUESTION("IMAGE_QUESTION");

    @EnumValue
    private final String code;
}
