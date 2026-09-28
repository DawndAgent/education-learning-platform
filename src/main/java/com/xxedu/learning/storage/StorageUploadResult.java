package com.xxedu.learning.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class StorageUploadResult {

    private final String objectKey;
    private final String url;
    private final String contentType;
    private final long size;
}
