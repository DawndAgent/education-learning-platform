package com.xxedu.learning.modules.content.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ContentPageQuery {

    private final List<Long> categoryIds;
    private final String contentType;
    private final String status;
    private final String keywordPattern;
    private final boolean orderByPublishTime;
    private final String scheduleFilter;
    private final LocalDateTime publishTimeFrom;
    private final LocalDateTime publishTimeTo;

    public ContentPageQuery(List<Long> categoryIds, String contentType, String status, String keywordPattern) {
        this(categoryIds, contentType, status, keywordPattern, false, null, null, null);
    }

    public ContentPageQuery(
            List<Long> categoryIds,
            String contentType,
            String status,
            String keywordPattern,
            boolean orderByPublishTime) {
        this(categoryIds, contentType, status, keywordPattern, orderByPublishTime, null, null, null);
    }

    public ContentPageQuery(
            List<Long> categoryIds,
            String contentType,
            String status,
            String keywordPattern,
            boolean orderByPublishTime,
            String scheduleFilter,
            LocalDateTime publishTimeFrom,
            LocalDateTime publishTimeTo) {
        this.categoryIds = categoryIds == null ? null : List.copyOf(categoryIds);
        this.contentType = contentType;
        this.status = status;
        this.keywordPattern = keywordPattern;
        this.orderByPublishTime = orderByPublishTime;
        this.scheduleFilter = scheduleFilter;
        this.publishTimeFrom = publishTimeFrom;
        this.publishTimeTo = publishTimeTo;
    }

    public List<Long> getCategoryIds() {
        return categoryIds == null ? null : new ArrayList<>(categoryIds);
    }

    public String getContentType() {
        return contentType;
    }

    public String getStatus() {
        return status;
    }

    public String getKeywordPattern() {
        return keywordPattern;
    }

    public boolean isOrderByPublishTime() {
        return orderByPublishTime;
    }

    public String getScheduleFilter() {
        return scheduleFilter;
    }

    public LocalDateTime getPublishTimeFrom() {
        return publishTimeFrom;
    }

    public LocalDateTime getPublishTimeTo() {
        return publishTimeTo;
    }
}
