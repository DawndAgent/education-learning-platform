package com.xxedu.learning.storage;

import java.io.InputStream;

/**
 * 预留给非腾讯云对象存储（S3 / OSS）。生产请使用 storage.type=cos。
 */
public class ObjectStorageService implements StorageService {

    @Override
    public StorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long size) {
        throw new UnsupportedOperationException("对象存储类型尚未实现，请将 storage.type 设为 local 或 cos");
    }

    @Override
    public void delete(String objectKey) {
        throw new UnsupportedOperationException("对象存储类型尚未实现，请将 storage.type 设为 local 或 cos");
    }

    @Override
    public String getUrl(String objectKey) {
        throw new UnsupportedOperationException("对象存储类型尚未实现，请将 storage.type 设为 local 或 cos");
    }
}
