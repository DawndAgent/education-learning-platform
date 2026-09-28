package com.xxedu.learning.modules.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    private Long contentId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String downloadUrl;
    private String previewUrl;
    private String description;
}
