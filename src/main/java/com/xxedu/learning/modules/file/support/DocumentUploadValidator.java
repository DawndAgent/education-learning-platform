package com.xxedu.learning.modules.file.support;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DocumentUploadValidator {

    public static final long MAX_BYTES = 20L * 1024 * 1024;

    private static final Tika TIKA = new Tika();
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    private static final Map<String, String> EXT_TO_TYPE = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    private static final Set<String> OOXML_COMPAT = Set.of(
            "application/x-tika-ooxml",
            "application/zip"
    );

    private DocumentUploadValidator() {
    }

    public static ValidatedDocument validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        long size = file.getSize();
        if (size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        if (size > MAX_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过20MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!EXT_TO_TYPE.containsKey(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX 文件");
        }

        String claimedType = normalizeType(file.getContentType());
        String expectedByExt = EXT_TO_TYPE.get(extension);
        if (claimedType != null && !ALLOWED_TYPES.contains(claimedType) && !OOXML_COMPAT.contains(claimedType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }
        if (claimedType != null && ALLOWED_TYPES.contains(claimedType) && !claimedType.equals(expectedByExt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型与扩展名不一致");
        }

        String detected;
        try (InputStream inputStream = file.getInputStream()) {
            detected = normalizeType(TIKA.detect(inputStream, originalName));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法识别文件类型");
        }
        if (detected == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容不是允许的文档格式");
        }
        if (!ALLOWED_TYPES.contains(detected) && !compatibleOoxml(detected, extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容不是允许的文档格式");
        }
        if (ALLOWED_TYPES.contains(detected) && !detected.equals(expectedByExt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容与扩展名不一致");
        }
        return new ValidatedDocument(safeDisplayName(originalName, extension), extension, expectedByExt, size);
    }

    private static boolean compatibleOoxml(String detected, String extension) {
        return OOXML_COMPAT.contains(detected)
                && Set.of("docx", "xlsx", "pptx").contains(extension);
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX 文件");
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
        String name = originalName == null ? "document." + extension : originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) {
            return "document." + extension;
        }
        return name.length() > 128 ? name.substring(name.length() - 128) : name;
    }

    public record ValidatedDocument(String fileName, String extension, String contentType, long size) {
    }
}
