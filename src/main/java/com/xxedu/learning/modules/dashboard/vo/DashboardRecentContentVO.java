package com.xxedu.learning.modules.dashboard.vo;

import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DashboardRecentContentVO {

    private Long id;
    private String title;
    private ContentType contentType;
    private String categoryName;
    private LocalDateTime publishTime;
}
