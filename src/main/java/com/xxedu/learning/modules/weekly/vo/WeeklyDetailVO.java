package com.xxedu.learning.modules.weekly.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WeeklyDetailVO {

    private Long contentId;
    private String title;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private String weekLabel;
    private String questionText;
    private String questionImageUrl;
    private String answerText;
    private String answerImageUrl;
    private String analysisText;
    private String analysisImageUrl;
}
