package com.xxedu.learning.modules.file.support;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

public final class VideoUploadValidator {

    public static final long MAX_BYTES = 50L * 1024 * 1024;

    private static final Tika TIKA = new Tika();
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "video/mp4",
            "video/quicktime",
            "application/mp4"
    );

    private VideoUploadValidator() {
    }

    public static ValidatedVideo validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        long size = file.getSize();
        if (size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        if (size > MAX_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频大小不能超过50MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!"mp4".equals(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 MP4 视频");
        }

        String claimedType = normalizeType(file.getContentType());
        if (claimedType != null && !ALLOWED_TYPES.contains(claimedType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }

        String detected;
        try (InputStream inputStream = file.getInputStream()) {
            detected = normalizeType(TIKA.detect(inputStream, originalName));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法识别文件类型");
        }
        if ((detected == null || !ALLOWED_TYPES.contains(detected)) && !hasFtypBox(file)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容不是允许的视频格式");
        }
        return new ValidatedVideo(safeDisplayName(originalName, extension), extension, "video/mp4", size);
    }

    private static boolean hasFtypBox(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            return header.length >= 8
                    && header[4] == 'f'
                    && header[5] == 't'
                    && header[6] == 'y'
                    && header[7] == 'p';
        } catch (IOException ex) {
            return false;
        }
    }

    private static String extensionOf(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        String name = originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 MP4 视频");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String type = contentType.trim().toLowerCase(Locale.ROOT);
        int semicolon = type.indexOf(';');
        if (semicolon > 0) {
            type = type.substring(0, semicolon).trim();
        }
        return type;
    }

    private static String safeDisplayName(String originalName, String extension) {
        String name = originalName == null ? "video." + extension : originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) {
            return "video." + extension;
        }
        return name.length() > 128 ? name.substring(name.length() - 128) : name;
    }

    public record ValidatedVideo(String fileName, String extension, String contentType, long size) {
    }
}
