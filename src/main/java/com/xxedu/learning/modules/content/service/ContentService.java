package com.xxedu.learning.modules.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.article.entity.Article;
import com.xxedu.learning.modules.article.mapper.ArticleMapper;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.common.util.HtmlSanitizer;
import com.xxedu.learning.common.util.HttpUrls;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.content.convert.ContentConverter;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.dto.ContentQueryRequest;
import com.xxedu.learning.modules.content.dto.ContentUpdateRequest;
import com.xxedu.learning.modules.content.dto.SchedulePublishRequest;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentScheduleFilter;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentDueRow;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.mapper.ContentPageQuery;
import com.xxedu.learning.modules.content.support.ScheduleTimes;
import com.xxedu.learning.modules.content.vo.ContentBatchResultVO;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import com.xxedu.learning.modules.document.entity.Document;
import com.xxedu.learning.modules.document.mapper.DocumentMapper;
import com.xxedu.learning.modules.question.entity.Question;
import com.xxedu.learning.modules.question.mapper.QuestionMapper;
import com.xxedu.learning.modules.video.entity.Video;
import com.xxedu.learning.modules.video.mapper.VideoMapper;
import com.xxedu.learning.modules.weekly.entity.WeeklyQuestion;
import com.xxedu.learning.modules.weekly.mapper.WeeklyQuestionMapper;
import com.xxedu.learning.security.AuthContext;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class ContentService {

    private static final String NOT_FOUND = "内容不存在";
    private static final String DRAFT_OFFLINE = "草稿不能下架";
    private static final String TYPE_LOCKED = "内容类型创建后不能修改";
    private static final String TYPE_UNSUPPORTED = "不支持的内容类型";
    private static final String PUBLISH_INCOMPLETE = "发布内容缺少必要信息";
    private static final String DELETE_PUBLISHED = "请先下架内容后再删除";
    private static final String STATUS_NOT_ALLOWED = "内容状态不允许";
    private static final String SCHEDULE_IN_PAST = "发布时间必须晚于当前时间";
    private static final String PUBLISHED_CANNOT_SCHEDULE = "已发布内容不能定时发布";
    private static final int DUE_BATCH_LIMIT = 100;
    private static final Set<ContentType> WRITABLE_TYPES = EnumSet.of(
            ContentType.ARTICLE, ContentType.VIDEO, ContentType.QUESTION,
            ContentType.WEEKLY, ContentType.DOCUMENT);

    private final ContentMapper contentMapper;
    private final ArticleMapper articleMapper;
    private final VideoMapper videoMapper;
    private final QuestionMapper questionMapper;
    private final WeeklyQuestionMapper weeklyQuestionMapper;
    private final DocumentMapper documentMapper;
    private final CategoryService categoryService;
    private final ContentConverter contentConverter;

    public PageResult<ContentListVO> publicPage(@Valid ContentQueryRequest request) {
        return page(request, true);
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public PageResult<ContentListVO> adminPage(@Valid ContentQueryRequest request) {
        return page(request, false);
    }

    public ContentDetailVO publicDetail(Long id) {
        Content content = requireContent(id);
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return contentConverter.toDetail(content);
    }

    /**
     * 已发布内容浏览量原子 +1。草稿 / 下线 / 不存在一律按不存在处理。
     */
    @Transactional
    public void recordView(Long id) {
        int updated = contentMapper.incrementViewCount(id);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public ContentDetailVO adminDetail(Long id) {
        return detailOf(requireContent(id));
    }

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public ContentDetailVO create(@Valid ContentCreateRequest request) {
        assertWritableType(request.getContentType());
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = new Content();
        content.setContentType(request.getContentType());
        content.setStatus(ContentStatus.DRAFT);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        applyCatalog(content, request.getTitle(), request.getCategoryId(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.insert(content);
        BizLogger.info("content.create", "id={} type={}", content.getId(), content.getContentType());
        return detailOf(requireContent(content.getId()));
    }

    @RequirePermission(PermissionCodes.CONTENT_UPDATE)
    @Transactional
    public ContentDetailVO update(Long id, @Valid ContentUpdateRequest request) {
        Content content = requireContent(id);
        if (request.getContentType() != content.getContentType()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, TYPE_LOCKED);
        }
        assertWritableType(content.getContentType());
        categoryService.assertContentCategory(request.getCategoryId(), false);
        applyCatalog(content, request.getTitle(), request.getCategoryId(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        BizLogger.info("content.update", "id={} type={}", content.getId(), content.getContentType());
        return detailOf(requireContent(id));
    }

    @RequirePermission(PermissionCodes.CONTENT_PUBLISH)
    @Transactional
    public ContentDetailVO publish(Long id) {
        Content content = requireContent(id);
        assertPublishable(content);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            if (content.getScheduledPublishTime() != null) {
                contentMapper.clearScheduledPublishTime(id, now);
            }
            BizLogger.info("content.publish", "id={} status={}", id, ContentStatus.PUBLISHED);
            return detailOf(requireContent(id));
        }
        int updated;
        if (content.getStatus() == ContentStatus.DRAFT) {
            updated = contentMapper.publishFromDraft(id, now);
        } else if (content.getStatus() == ContentStatus.OFFLINE) {
            updated = contentMapper.publishFromOffline(id, now);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, STATUS_NOT_ALLOWED);
        }
        if (updated == 0) {
            BizLogger.info("content.publish", "id={} result=skipped", id);
            return detailOf(requireContent(id));
        }
        content.setStatus(ContentStatus.PUBLISHED);
        content.setPublishTime(now);
        content.setScheduledPublishTime(null);
        syncVideoStatus(content);
        BizLogger.info("content.publish", "id={} status={}", id, ContentStatus.PUBLISHED);
        return detailOf(requireContent(id));
    }

    @RequirePermission(PermissionCodes.CONTENT_PUBLISH)
    @Transactional
    public ContentDetailVO schedulePublish(Long id, @Valid SchedulePublishRequest request) {
        Content content = requireContent(id);
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PUBLISHED_CANNOT_SCHEDULE);
        }
        if (content.getStatus() != ContentStatus.DRAFT && content.getStatus() != ContentStatus.OFFLINE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, STATUS_NOT_ALLOWED);
        }
        LocalDateTime publishTime = ScheduleTimes.parse(request.getPublishTime());
        if (!publishTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, SCHEDULE_IN_PAST);
        }
        assertPublishable(content);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        int updated = contentMapper.update(null, Wrappers.<Content>lambdaUpdate()
                .set(Content::getStatus, ContentStatus.DRAFT)
                .set(Content::getScheduledPublishTime, publishTime)
                .set(Content::getUpdatedAt, now)
                .eq(Content::getId, id)
                .in(Content::getStatus, ContentStatus.DRAFT, ContentStatus.OFFLINE));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, STATUS_NOT_ALLOWED);
        }
        BizLogger.info("content.schedule", "id={} type={} publishTime={}",
                id, content.getContentType(), publishTime);
        return detailOf(requireContent(id));
    }

    @RequirePermission(PermissionCodes.CONTENT_PUBLISH)
    @Transactional
    public ContentDetailVO cancelScheduledPublish(Long id) {
        Content content = requireContent(id);
        if (content.getStatus() != ContentStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, STATUS_NOT_ALLOWED);
        }
        if (content.getScheduledPublishTime() != null) {
            contentMapper.clearScheduledPublishTime(id, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        }
        BizLogger.info("content.schedule.cancel", "id={}", id);
        return detailOf(requireContent(id));
    }

    /**
     * 由调度任务逐条调用。调度器本身无事务，因此每条内容单独提交；
     * 校验失败只回滚当前内容。
     */
    @Transactional
    public void publishScheduled(Long id) {
        Content content = contentMapper.selectOne(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getTitle, Content::getContentType, Content::getCategoryId,
                        Content::getStatus, Content::getScheduledPublishTime)
                .eq(Content::getId, id));
        if (content == null || content.getStatus() != ContentStatus.DRAFT || content.getScheduledPublishTime() == null) {
            return;
        }
        if (content.getScheduledPublishTime().isAfter(LocalDateTime.now())) {
            return;
        }
        assertPublishable(content);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        int updated = contentMapper.publishFromDraft(id, now);
        if (updated == 0) {
            return;
        }
        content.setStatus(ContentStatus.PUBLISHED);
        content.setPublishTime(now);
        syncVideoStatus(content);
        BizLogger.info("content.schedulePublish.success", "contentId={} contentType={} publishTime={} result=success",
                id, content.getContentType(), now);
    }

    public List<ContentDueRow> findDueDrafts(LocalDateTime now, int limit) {
        int size = Math.min(Math.max(limit, 1), DUE_BATCH_LIMIT);
        return contentMapper.selectDueDrafts(now, size);
    }

    @RequirePermission(PermissionCodes.CONTENT_OFFLINE)
    @Transactional
    public ContentDetailVO offline(Long id) {
        Content content = requireContent(id);
        offlineInternal(content);
        BizLogger.info("content.offline", "id={} status={}", content.getId(), content.getStatus());
        return detailOf(content);
    }

    @RequirePermission(PermissionCodes.CONTENT_DELETE)
    @Transactional
    public void delete(Long id) {
        Content content = requireContent(id);
        ContentType type = content.getContentType();
        deleteInternal(content);
        BizLogger.info("content.delete", "id={} type={}", id, type);
    }

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public ContentDetailVO duplicate(Long id) {
        Content source = requireContent(id);
        assertWritableType(source.getContentType());
        if (source.getContentType() == ContentType.VIDEO && !AuthContext.hasPermission(PermissionCodes.VIDEO_MANAGE)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Content copy = new Content();
        copy.setTitle(source.getTitle());
        copy.setContentType(source.getContentType());
        copy.setCategoryId(source.getCategoryId());
        copy.setCoverUrl(source.getCoverUrl());
        copy.setSummary(source.getSummary());
        copy.setSort(source.getSort() == null ? 0 : source.getSort());
        copy.setStatus(ContentStatus.DRAFT);
        copy.setViewCount(0L);
        copy.setFavoriteCount(0L);
        copy.setPublishTime(null);
        contentMapper.insert(copy);

        if (source.getContentType() == ContentType.ARTICLE) {
            duplicateArticle(source.getId(), copy.getId());
        } else if (source.getContentType() == ContentType.VIDEO) {
            duplicateVideo(source.getId(), copy.getId());
        } else if (source.getContentType() == ContentType.QUESTION) {
            duplicateQuestion(source.getId(), copy.getId());
        } else if (source.getContentType() == ContentType.WEEKLY) {
            duplicateWeekly(source.getId(), copy.getId());
        } else if (source.getContentType() == ContentType.DOCUMENT) {
            duplicateDocument(source.getId(), copy.getId());
        }
        BizLogger.info("content.duplicate", "sourceId={} newId={} type={}", id, copy.getId(), copy.getContentType());
        return detailOf(requireContent(copy.getId()));
    }

    @RequirePermission(PermissionCodes.CONTENT_OFFLINE)
    @Transactional
    public ContentBatchResultVO batchOffline(List<Long> ids) {
        List<Long> distinct = normalizeIds(ids);
        List<Content> contents = new ArrayList<>(distinct.size());
        for (Long id : distinct) {
            Content content = requireContent(id);
            if (content.getStatus() != ContentStatus.PUBLISHED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "只能下线已发布内容");
            }
            contents.add(content);
        }
        for (Content content : contents) {
            offlineInternal(content);
        }
        BizLogger.info("content.batchOffline", "count={}", contents.size());
        return new ContentBatchResultVO(contents.size(), 0);
    }

    @RequirePermission(PermissionCodes.CONTENT_DELETE)
    @Transactional
    public ContentBatchResultVO batchDelete(List<Long> ids) {
        List<Long> distinct = normalizeIds(ids);
        List<Content> contents = new ArrayList<>(distinct.size());
        for (Long id : distinct) {
            Content content = requireContent(id);
            if (content.getStatus() == ContentStatus.PUBLISHED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, DELETE_PUBLISHED);
            }
            contents.add(content);
        }
        for (Content content : contents) {
            deleteInternal(content);
        }
        BizLogger.info("content.batchDelete", "count={}", contents.size());
        return new ContentBatchResultVO(contents.size(), 0);
    }

    public Content requireContent(Long id) {
        Content content = contentMapper.selectOne(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getTitle, Content::getContentType, Content::getCategoryId,
                        Content::getCoverUrl, Content::getSummary, Content::getStatus, Content::getSort,
                        Content::getViewCount, Content::getFavoriteCount, Content::getPublishTime,
                        Content::getScheduledPublishTime, Content::getCreatedAt, Content::getUpdatedAt)
                .eq(Content::getId, id));
        if (content == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private void offlineInternal(Content content) {
        if (content.getStatus() == ContentStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DRAFT_OFFLINE);
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        contentMapper.update(null, Wrappers.<Content>lambdaUpdate()
                .set(Content::getStatus, ContentStatus.OFFLINE)
                .set(Content::getScheduledPublishTime, null)
                .set(Content::getUpdatedAt, now)
                .eq(Content::getId, content.getId())
                .ne(Content::getStatus, ContentStatus.DRAFT));
        if (content.getStatus() != ContentStatus.OFFLINE) {
            content.setStatus(ContentStatus.OFFLINE);
            content.setScheduledPublishTime(null);
            syncVideoStatus(content);
        }
    }

    private void deleteInternal(Content content) {
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DELETE_PUBLISHED);
        }
        Long id = content.getId();
        if (content.getContentType() == ContentType.ARTICLE) {
            articleMapper.delete(Wrappers.<Article>lambdaQuery().eq(Article::getContentId, id));
        } else if (content.getContentType() == ContentType.VIDEO) {
            videoMapper.delete(Wrappers.<Video>lambdaQuery().eq(Video::getContentId, id));
        } else if (content.getContentType() == ContentType.QUESTION) {
            questionMapper.delete(Wrappers.<Question>lambdaQuery().eq(Question::getContentId, id));
        } else if (content.getContentType() == ContentType.WEEKLY) {
            weeklyQuestionMapper.delete(Wrappers.<WeeklyQuestion>lambdaQuery().eq(WeeklyQuestion::getContentId, id));
        } else if (content.getContentType() == ContentType.DOCUMENT) {
            documentMapper.delete(Wrappers.<Document>lambdaQuery().eq(Document::getContentId, id));
        }
        contentMapper.deleteById(id);
    }

    private void duplicateArticle(Long sourceContentId, Long newContentId) {
        Article source = articleMapper.selectOne(Wrappers.<Article>lambdaQuery()
                .select(Article::getId, Article::getBody, Article::getAuthor, Article::getSource)
                .eq(Article::getContentId, sourceContentId));
        if (source == null) {
            return;
        }
        Article copy = new Article();
        copy.setContentId(newContentId);
        copy.setBody(source.getBody() == null ? "" : source.getBody());
        copy.setAuthor(source.getAuthor());
        copy.setSource(source.getSource());
        articleMapper.insert(copy);
    }

    private void duplicateVideo(Long sourceContentId, Long newContentId) {
        Video source = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .select(Video::getId, Video::getTitle, Video::getCoverUrl, Video::getSourceType,
                        Video::getVideoUrl, Video::getQrCodeUrl, Video::getDuration)
                .eq(Video::getContentId, sourceContentId));
        if (source == null) {
            return;
        }
        Video copy = new Video();
        copy.setContentId(newContentId);
        copy.setTitle(source.getTitle());
        copy.setCoverUrl(source.getCoverUrl());
        copy.setSourceType(source.getSourceType());
        copy.setVideoUrl(source.getVideoUrl());
        copy.setQrCodeUrl(source.getQrCodeUrl());
        copy.setDuration(source.getDuration());
        copy.setStatus(ContentStatus.DRAFT);
        videoMapper.insert(copy);
    }

    private void duplicateQuestion(Long sourceContentId, Long newContentId) {
        Question source = questionMapper.selectOne(Wrappers.<Question>lambdaQuery()
                .select(Question::getId, Question::getQuestionType, Question::getQuestionText,
                        Question::getQuestionImageUrl, Question::getAnswerText, Question::getAnswerImageUrl,
                        Question::getAnalysisText, Question::getAnalysisImageUrl, Question::getDifficulty)
                .eq(Question::getContentId, sourceContentId));
        if (source == null) {
            return;
        }
        Question copy = new Question();
        copy.setContentId(newContentId);
        copy.setQuestionType(source.getQuestionType());
        copy.setQuestionText(source.getQuestionText());
        copy.setQuestionImageUrl(source.getQuestionImageUrl());
        copy.setAnswerText(source.getAnswerText());
        copy.setAnswerImageUrl(source.getAnswerImageUrl());
        copy.setAnalysisText(source.getAnalysisText());
        copy.setAnalysisImageUrl(source.getAnalysisImageUrl());
        copy.setDifficulty(source.getDifficulty());
        questionMapper.insert(copy);
    }

    private void duplicateWeekly(Long sourceContentId, Long newContentId) {
        WeeklyQuestion source = weeklyQuestionMapper.selectOne(Wrappers.<WeeklyQuestion>lambdaQuery()
                .select(WeeklyQuestion::getId, WeeklyQuestion::getWeekLabel, WeeklyQuestion::getQuestionText,
                        WeeklyQuestion::getQuestionImageUrl, WeeklyQuestion::getAnswerText,
                        WeeklyQuestion::getAnswerImageUrl, WeeklyQuestion::getAnalysisText,
                        WeeklyQuestion::getAnalysisImageUrl)
                .eq(WeeklyQuestion::getContentId, sourceContentId));
        if (source == null) {
            return;
        }
        WeeklyQuestion copy = new WeeklyQuestion();
        copy.setContentId(newContentId);
        copy.setWeekLabel(source.getWeekLabel());
        copy.setQuestionText(source.getQuestionText());
        copy.setQuestionImageUrl(source.getQuestionImageUrl());
        copy.setAnswerText(source.getAnswerText());
        copy.setAnswerImageUrl(source.getAnswerImageUrl());
        copy.setAnalysisText(source.getAnalysisText());
        copy.setAnalysisImageUrl(source.getAnalysisImageUrl());
        weeklyQuestionMapper.insert(copy);
    }

    private void duplicateDocument(Long sourceContentId, Long newContentId) {
        Document source = documentMapper.selectOne(Wrappers.<Document>lambdaQuery()
                .select(Document::getId, Document::getFileUrl, Document::getFileName, Document::getFileSize,
                        Document::getFileType, Document::getDownloadUrl, Document::getPreviewUrl,
                        Document::getDescription)
                .eq(Document::getContentId, sourceContentId));
        if (source == null) {
            return;
        }
        Document copy = new Document();
        copy.setContentId(newContentId);
        copy.setFileUrl(source.getFileUrl());
        copy.setFileName(source.getFileName());
        copy.setFileSize(source.getFileSize());
        copy.setFileType(source.getFileType());
        copy.setDownloadUrl(source.getDownloadUrl());
        copy.setPreviewUrl(source.getPreviewUrl());
        copy.setDescription(source.getDescription());
        documentMapper.insert(copy);
    }

    private static List<Long> normalizeIds(List<Long> ids) {
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
        return distinct;
    }

    private void syncVideoStatus(Content content) {
        if (content.getContentType() != ContentType.VIDEO) {
            return;
        }
        Video video = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .select(Video::getId, Video::getStatus)
                .eq(Video::getContentId, content.getId()));
        if (video == null) {
            return;
        }
        if (video.getStatus() == content.getStatus()) {
            return;
        }
        video.setStatus(content.getStatus());
        videoMapper.updateById(video);
    }

    private PageResult<ContentListVO> page(ContentQueryRequest request, boolean publicQuery) {
        List<Long> categoryIds = null;
        if (request.getCategoryId() != null) {
            categoryIds = categoryService.selfAndDescendantIds(request.getCategoryId());
            if (categoryIds.isEmpty()) {
                return PageResult.of(request.getPageNum(), request.getPageSize(), 0, List.of());
            }
        }
        ContentStatus status = publicQuery ? ContentStatus.PUBLISHED : request.getStatus();
        boolean orderByPublishTime = publicQuery && "publishTime".equalsIgnoreCase(request.getSort());
        ContentScheduleFilter schedule = publicQuery ? null : request.getSchedule();
        ContentPageQuery query = new ContentPageQuery(
                categoryIds,
                request.getContentType() == null ? null : request.getContentType().getCode(),
                status == null ? null : status.getCode(),
                likePattern(request.getKeyword()),
                orderByPublishTime,
                schedule == null ? null : schedule.name(),
                publicQuery ? null : request.getPublishTimeFrom(),
                publicQuery ? null : request.getPublishTimeTo());
        IPage<Content> result = contentMapper.selectContentPage(
                new Page<>(request.getPageNum(), request.getPageSize()), query);
        List<ContentListVO> records = result.getRecords().stream().map(contentConverter::toList).toList();
        Map<Long, String> categoryNames = categoryService.namesByIds(
                records.stream().map(ContentListVO::getCategoryId).toList());
        for (ContentListVO record : records) {
            record.setCategoryName(categoryNames.get(record.getCategoryId()));
        }
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    private ContentDetailVO detailOf(Content content) {
        ContentDetailVO detail = contentConverter.toDetail(content);
        if (content.getCategoryId() != null) {
            detail.setCategoryName(categoryService.namesByIds(List.of(content.getCategoryId()))
                    .get(content.getCategoryId()));
        }
        return detail;
    }

    private void applyCatalog(Content content, String title, Long categoryId, String coverUrl, String summary, Integer sort) {
        content.setTitle(title.trim());
        content.setCategoryId(categoryId);
        content.setCoverUrl(TextValues.trimToNull(coverUrl));
        content.setSummary(TextValues.trimToNull(summary));
        content.setSort(sort);
    }

    private void assertWritableType(ContentType contentType) {
        if (contentType == null || !WRITABLE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, TYPE_UNSUPPORTED);
        }
    }

    private void assertPublishable(Content content) {
        if (content.getTitle() == null || content.getTitle().isBlank()
                || content.getCategoryId() == null
                || content.getContentType() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PUBLISH_INCOMPLETE);
        }
        assertWritableType(content.getContentType());
        categoryService.assertContentCategory(content.getCategoryId(), true);
        if (content.getContentType() == ContentType.ARTICLE) {
            assertArticlePublishable(content.getId());
        } else if (content.getContentType() == ContentType.VIDEO) {
            assertVideoPublishable(content.getId());
        } else if (content.getContentType() == ContentType.QUESTION) {
            assertQuestionPublishable(content.getId());
        } else if (content.getContentType() == ContentType.WEEKLY) {
            assertWeeklyPublishable(content.getId());
        } else if (content.getContentType() == ContentType.DOCUMENT) {
            assertDocumentPublishable(content.getId());
        }
    }

    private void assertArticlePublishable(Long contentId) {
        Article article = articleMapper.selectOne(Wrappers.<Article>lambdaQuery()
                .select(Article::getId, Article::getBody)
                .eq(Article::getContentId, contentId));
        if (article == null || !HtmlSanitizer.hasText(article.getBody())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章正文不能为空");
        }
    }

    private void assertVideoPublishable(Long contentId) {
        Video video = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .select(Video::getId, Video::getSourceType, Video::getVideoUrl, Video::getQrCodeUrl)
                .eq(Video::getContentId, contentId));
        if (video == null || video.getSourceType() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频来源不能为空");
        }
        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频地址不能为空");
        }
        if (video.getSourceType().inAppPlayback()) {
            if (!HttpUrls.isMediaUrl(video.getVideoUrl())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "视频地址不合法");
            }
            if (video.getQrCodeUrl() != null && !video.getQrCodeUrl().isBlank()
                    && !HttpUrls.isMediaUrl(video.getQrCodeUrl())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "二维码地址不合法");
            }
            return;
        }
        if (!HttpUrls.isHttp(video.getVideoUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频地址不合法");
        }
        if (video.getQrCodeUrl() == null || video.getQrCodeUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "二维码地址不能为空");
        }
        if (!HttpUrls.isMediaUrl(video.getQrCodeUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "二维码地址不合法");
        }
    }

    private void assertQuestionPublishable(Long contentId) {
        Question question = questionMapper.selectOne(Wrappers.<Question>lambdaQuery()
                .select(Question::getId, Question::getQuestionText, Question::getQuestionImageUrl,
                        Question::getAnswerText, Question::getAnswerImageUrl)
                .eq(Question::getContentId, contentId));
        if (question == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目内容不能为空");
        }
        assertQuestionAndAnswerPresent(question.getQuestionText(), question.getQuestionImageUrl(),
                question.getAnswerText(), question.getAnswerImageUrl());
    }

    private void assertWeeklyPublishable(Long contentId) {
        WeeklyQuestion weekly = weeklyQuestionMapper.selectOne(Wrappers.<WeeklyQuestion>lambdaQuery()
                .select(WeeklyQuestion::getId, WeeklyQuestion::getWeekLabel, WeeklyQuestion::getQuestionText,
                        WeeklyQuestion::getQuestionImageUrl, WeeklyQuestion::getAnswerText,
                        WeeklyQuestion::getAnswerImageUrl)
                .eq(WeeklyQuestion::getContentId, contentId));
        if (weekly == null || weekly.getWeekLabel() == null || weekly.getWeekLabel().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "周次标签不能为空");
        }
        assertQuestionAndAnswerPresent(weekly.getQuestionText(), weekly.getQuestionImageUrl(),
                weekly.getAnswerText(), weekly.getAnswerImageUrl());
    }

    private void assertDocumentPublishable(Long contentId) {
        Document document = documentMapper.selectOne(Wrappers.<Document>lambdaQuery()
                .select(Document::getId, Document::getFileUrl, Document::getFileName)
                .eq(Document::getContentId, contentId));
        if (document == null
                || document.getFileUrl() == null || document.getFileUrl().isBlank()
                || document.getFileName() == null || document.getFileName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "资料文件不能为空");
        }
    }

    private static void assertQuestionAndAnswerPresent(String questionText, String questionImageUrl,
                                                       String answerText, String answerImageUrl) {
        if (!hasTextOrUrl(questionText, questionImageUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目内容不能为空");
        }
        if (!hasTextOrUrl(answerText, answerImageUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "答案不能为空");
        }
    }

    private static boolean hasTextOrUrl(String text, String url) {
        return (text != null && !text.isBlank()) || (url != null && !url.isBlank());
    }

    public static String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
