package com.xxedu.learning.storage;

import java.io.InputStream;

public interface StorageService {

    StorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long size);

    void delete(String objectKey);

    String getUrl(String objectKey);
}
