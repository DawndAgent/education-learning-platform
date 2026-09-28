package com.xxedu.learning.common.util;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;

import java.net.URI;

public final class HttpUrls {

    private HttpUrls() {
    }

    public static void requireHttpIfPresent(String value, String message) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!isHttp(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    public static void requireMediaUrlIfPresent(String value, String message) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!isMediaUrl(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    public static boolean isMediaUrl(String value) {
        if (isHttp(value)) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("/")
                && !trimmed.startsWith("//")
                && !trimmed.contains("..")
                && !trimmed.toLowerCase().startsWith("/javascript:");
    }

    public static boolean isHttp(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null
                    && !host.isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
