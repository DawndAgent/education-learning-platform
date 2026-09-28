package com.xxedu.learning.modules.content.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {

    ARTICLE("ARTICLE"),
    VIDEO("VIDEO"),
    QUESTION("QUESTION"),
    TOPIC("TOPIC"),
    WEEKLY("WEEKLY"),
    DOCUMENT("DOCUMENT");

    @EnumValue
    private final String code;
}
