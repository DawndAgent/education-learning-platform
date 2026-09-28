package com.xxedu.learning.modules.content.service;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.content.mapper.ContentDueRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 逐条调用 {@link ContentService#publishScheduled(Long)}，保证单条失败不影响同批其他内容。
 */
@Service
@RequiredArgsConstructor
public class ContentPublishJob {

    private static final int BATCH_LIMIT = 100;

    private final ContentService contentService;

    public void runOnce() {
        List<ContentDueRow> due = contentService.findDueDrafts(LocalDateTime.now(), BATCH_LIMIT);
        for (ContentDueRow row : due) {
            try {
                contentService.publishScheduled(row.getId());
            } catch (BusinessException ex) {
                BizLogger.warn("content.schedulePublish.fail", "contentId={} contentType={} reason={}",
                        row.getId(), row.getContentType(), ex.getMessage());
            } catch (RuntimeException ex) {
                BizLogger.warn("content.schedulePublish.fail", "contentId={} contentType={} reason={}",
                        row.getId(), row.getContentType(), ex.getClass().getSimpleName());
            }
        }
    }
}
