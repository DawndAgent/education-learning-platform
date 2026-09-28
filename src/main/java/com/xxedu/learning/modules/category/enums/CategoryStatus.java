package com.xxedu.learning.modules.category.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryStatus {

    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    @EnumValue
    private final String code;
}
