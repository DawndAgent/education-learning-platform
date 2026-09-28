package com.xxedu.learning.modules.content.service;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.content.vo.ContentBatchResultVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContentBatchPublishService {

    private final ContentService contentService;

    @RequirePermission(PermissionCodes.CONTENT_PUBLISH)
    public ContentBatchResultVO batchPublish(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容 ID 不能为空");
        }
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容 ID 不能为空");
        }
        if (distinct.size() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次最多操作 100 条");
        }
        int success = 0;
        int failed = 0;
        for (Long id : distinct) {
            try {
                contentService.publish(id);
                success++;
            } catch (BusinessException ex) {
                failed++;
                BizLogger.warn("content.batchPublish.fail", "contentId={} reason={}", id, ex.getMessage());
            }
        }
        BizLogger.info("content.batchPublish", "success={} failed={}", success, failed);
        return new ContentBatchResultVO(success, failed);
    }
}
