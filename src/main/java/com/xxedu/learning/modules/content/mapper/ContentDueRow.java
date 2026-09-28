package com.xxedu.learning.modules.content.mapper;

import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContentDueRow {

    private Long id;
    private ContentType contentType;
    private LocalDateTime scheduledPublishTime;
}
