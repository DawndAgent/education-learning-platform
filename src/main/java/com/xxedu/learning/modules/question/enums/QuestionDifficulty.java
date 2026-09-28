package com.xxedu.learning.modules.question.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionDifficulty {

    EASY("EASY"),
    MEDIUM("MEDIUM"),
    HARD("HARD");

    @EnumValue
    private final String code;
}
