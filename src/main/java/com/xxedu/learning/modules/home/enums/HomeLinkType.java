package com.xxedu.learning.modules.home.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomeLinkType {

    CONTENT("CONTENT"),
    TOPIC("TOPIC"),
    URL("URL"),
    NONE("NONE");

    @EnumValue
    private final String code;
}
