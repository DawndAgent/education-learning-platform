package com.xxedu.learning.modules.home.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendType {

    CONTENT("CONTENT"),
    TOPIC("TOPIC");

    @EnumValue
    private final String code;
}
