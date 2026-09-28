package com.xxedu.learning.modules.file.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadVO {

    private String url;
    private String objectKey;
    private String fileName;
    private String contentType;
    private long size;
}
