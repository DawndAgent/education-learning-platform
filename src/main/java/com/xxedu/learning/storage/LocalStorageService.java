package com.xxedu.learning.storage;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;
    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Path.of(properties.getLocal().getBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        BizLogger.info("storage.local.init", "basePath={}", root);
    }

    @Override
    public StorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long size) {
        Path target = resolveUnderRoot(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件存储失败");
        }
        return new StorageUploadResult(objectKey, getUrl(objectKey), contentType, size);
    }

    @Override
    public void delete(String objectKey) {
        Path target = resolveUnderRoot(objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败");
        }
    }

    @Override
    public String getUrl(String objectKey) {
        String prefix = properties.getLocal().getPublicUrlPrefix();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return prefix + "/" + key;
    }

    public Path resolveUnderRoot(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对象键不能为空");
        }
        String normalized = objectKey.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法的对象键");
        }
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法的对象键");
        }
        return target;
    }

    public Path rootPath() {
        return root;
    }
}
