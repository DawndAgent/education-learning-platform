package com.xxedu.learning.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeleteFlag {

    NOT_DELETED(0),
    DELETED(1);

    private final int code;
}
