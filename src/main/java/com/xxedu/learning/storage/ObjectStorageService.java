package com.xxedu.learning.storage;

import java.io.InputStream;

/**
 * 对象存储预留实现。本 Sprint 不接入 COS / OSS / S3 SDK。
 */
public class ObjectStorageService implements StorageService {

    @Override
    public StorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long size) {
        throw new UnsupportedOperationException("对象存储尚未配置，请将 storage.type 设为 local");
    }

    @Override
    public void delete(String objectKey) {
        throw new UnsupportedOperationException("对象存储尚未配置，请将 storage.type 设为 local");
    }

    @Override
    public String getUrl(String objectKey) {
        throw new UnsupportedOperationException("对象存储尚未配置，请将 storage.type 设为 local");
    }
}
