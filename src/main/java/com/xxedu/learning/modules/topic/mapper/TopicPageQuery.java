package com.xxedu.learning.modules.topic.mapper;

import java.util.ArrayList;
import java.util.List;

public final class TopicPageQuery {

    private final List<Long> categoryIds;
    private final String status;
    private final String keywordPattern;
    private final boolean publishedOnly;

    public TopicPageQuery(List<Long> categoryIds, String status, String keywordPattern, boolean publishedOnly) {
        this.categoryIds = categoryIds == null ? null : List.copyOf(categoryIds);
        this.status = status;
        this.keywordPattern = keywordPattern;
        this.publishedOnly = publishedOnly;
    }

    public List<Long> getCategoryIds() {
        return categoryIds == null ? null : new ArrayList<>(categoryIds);
    }

    public String getStatus() {
        return status;
    }

    public String getKeywordPattern() {
        return keywordPattern;
    }

    public boolean isPublishedOnly() {
        return publishedOnly;
    }
}
