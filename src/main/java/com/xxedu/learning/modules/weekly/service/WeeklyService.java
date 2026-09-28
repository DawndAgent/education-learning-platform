package com.xxedu.learning.modules.weekly.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.weekly.convert.WeeklyConverter;
import com.xxedu.learning.modules.weekly.dto.WeeklyCreateRequest;
import com.xxedu.learning.modules.weekly.dto.WeeklyUpdateRequest;
import com.xxedu.learning.modules.weekly.entity.WeeklyQuestion;
import com.xxedu.learning.modules.weekly.mapper.WeeklyQuestionMapper;
import com.xxedu.learning.modules.weekly.vo.WeeklyDetailVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class WeeklyService {

    private static final String NOT_FOUND = "每周一题不存在";

    private final ContentMapper contentMapper;
    private final WeeklyQuestionMapper weeklyQuestionMapper;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final WeeklyConverter weeklyConverter;

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public WeeklyDetailVO create(@Valid WeeklyCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = newContent(request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.insert(content);
        WeeklyQuestion weekly = new WeeklyQuestion();
        weekly.setContentId(content.getId());
        applyWeekly(weekly, request.getWeekLabel(), request.getQuestionText(), request.getQuestionImageUrl(),
                request.getAnswerText(), request.getAnswerImageUrl(), request.getAnalysisText(),
                request.getAnalysisImageUrl());
        weeklyQuestionMapper.insert(weekly);
        BizLogger.info("weekly.create", "contentId={}", content.getId());
        return weeklyConverter.toDetail(content, weekly);
    }

    @RequirePermission(PermissionCodes.CONTENT_UPDATE)
    @Transactional
    public WeeklyDetailVO update(Long contentId, @Valid WeeklyUpdateRequest request) {
        Content content = requireWeeklyContent(contentId, false);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        WeeklyQuestion weekly = findWeekly(contentId);
        boolean creating = weekly == null;
        if (creating) {
            weekly = new WeeklyQuestion();
            weekly.setContentId(contentId);
        }
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        applyWeekly(weekly, request.getWeekLabel(), request.getQuestionText(), request.getQuestionImageUrl(),
                request.getAnswerText(), request.getAnswerImageUrl(), request.getAnalysisText(),
                request.getAnalysisImageUrl());
        if (creating) {
            weeklyQuestionMapper.insert(weekly);
        } else {
            weeklyQuestionMapper.updateById(weekly);
        }
        BizLogger.info("weekly.update", "contentId={}", contentId);
        return weeklyConverter.toDetail(content, weekly);
    }

    public WeeklyDetailVO publicDetail(Long contentId) {
        Content content = requireWeeklyContent(contentId, true);
        return weeklyConverter.toDetail(content, requireWeekly(contentId));
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public WeeklyDetailVO adminDetail(Long contentId) {
        Content content = requireWeeklyContent(contentId, false);
        WeeklyQuestion weekly = findWeekly(contentId);
        if (weekly == null) {
            weekly = new WeeklyQuestion();
            weekly.setContentId(contentId);
        }
        return weeklyConverter.toDetail(content, weekly);
    }

    private void applyWeekly(WeeklyQuestion weekly, String weekLabel, String questionText, String questionImageUrl,
                             String answerText, String answerImageUrl, String analysisText, String analysisImageUrl) {
        weekly.setWeekLabel(TextValues.trimToNull(weekLabel));
        weekly.setQuestionText(TextValues.trimToNull(questionText));
        weekly.setQuestionImageUrl(TextValues.trimToNull(questionImageUrl));
        weekly.setAnswerText(TextValues.trimToNull(answerText));
        weekly.setAnswerImageUrl(TextValues.trimToNull(answerImageUrl));
        weekly.setAnalysisText(TextValues.trimToNull(analysisText));
        weekly.setAnalysisImageUrl(TextValues.trimToNull(analysisImageUrl));
    }

    private Content requireWeeklyContent(Long contentId, boolean publishedOnly) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.WEEKLY) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        if (publishedOnly && content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private WeeklyQuestion requireWeekly(Long contentId) {
        WeeklyQuestion weekly = findWeekly(contentId);
        if (weekly == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return weekly;
    }

    private WeeklyQuestion findWeekly(Long contentId) {
        return weeklyQuestionMapper.selectOne(Wrappers.<WeeklyQuestion>lambdaQuery()
                .select(WeeklyQuestion::getId, WeeklyQuestion::getContentId, WeeklyQuestion::getWeekLabel,
                        WeeklyQuestion::getQuestionText, WeeklyQuestion::getQuestionImageUrl,
                        WeeklyQuestion::getAnswerText, WeeklyQuestion::getAnswerImageUrl,
                        WeeklyQuestion::getAnalysisText, WeeklyQuestion::getAnalysisImageUrl)
                .eq(WeeklyQuestion::getContentId, contentId));
    }

    private Content newContent(Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        Content content = new Content();
        content.setContentType(ContentType.WEEKLY);
        content.setStatus(ContentStatus.DRAFT);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        applyCatalog(content, categoryId, title, coverUrl, summary, sort);
        return content;
    }

    private void applyCatalog(Content content, Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        content.setCategoryId(categoryId);
        content.setTitle(title.trim());
        content.setCoverUrl(TextValues.trimToNull(coverUrl));
        content.setSummary(TextValues.trimToNull(summary));
        content.setSort(sort);
    }
}
