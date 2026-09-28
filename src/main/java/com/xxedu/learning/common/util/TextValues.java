package com.xxedu.learning.common.util;

public final class TextValues {

    private TextValues() {
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
