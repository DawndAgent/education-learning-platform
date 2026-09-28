package com.xxedu.learning.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldErrorVO {

    private final String field;
    private final String message;
}
