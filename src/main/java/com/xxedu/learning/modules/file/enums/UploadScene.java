package com.xxedu.learning.modules.file.enums;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;

public enum UploadScene {
    ARTICLE("articles"),
    COVER("covers"),
    VIDEO("videos"),
    VIDEO_FILE("local"),
    QRCODE("qrcodes"),
    QUESTION("questions"),
    WEEKLY("weeklies"),
    DOCUMENT("documents");

    private final String folder;

    UploadScene(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }

    public static UploadScene from(String value) {
        if (value == null || value.isBlank()) {
            return COVER;
        }
        try {
            return UploadScene.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的上传场景");
        }
    }
}
