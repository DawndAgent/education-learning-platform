package com.xxedu.learning.modules.document.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentDetailVO {

    private Long contentId;
    private String title;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String downloadUrl;
    private String previewUrl;
    private String description;
}
