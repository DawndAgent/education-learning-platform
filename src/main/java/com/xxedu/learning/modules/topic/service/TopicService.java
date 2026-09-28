package com.xxedu.learning.modules.topic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicContentSortRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemBatchRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemSortRequest;
import com.xxedu.learning.modules.topic.dto.TopicQueryRequest;
import com.xxedu.learning.modules.topic.dto.TopicUpdateRequest;
import com.xxedu.learning.modules.topic.entity.Topic;
import com.xxedu.learning.modules.topic.entity.TopicContent;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import com.xxedu.learning.modules.topic.mapper.TopicContentCountRow;
import com.xxedu.learning.modules.topic.mapper.TopicContentMapper;
import com.xxedu.learning.modules.topic.mapper.TopicContentRow;
import com.xxedu.learning.modules.topic.mapper.TopicMapper;
import com.xxedu.learning.modules.topic.mapper.TopicPageQuery;
import com.xxedu.learning.modules.topic.vo.PublicTopicCategoryVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicContentVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicDetailVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicListVO;
import com.xxedu.learning.modules.topic.vo.TopicContentItemVO;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.modules.topic.vo.TopicItemBatchResultVO;
import com.xxedu.learning.modules.topic.vo.TopicListVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class TopicService {

    private static final String NOT_FOUND = "专题不存在";
    private static final String DUPLICATE_CODE = "专题编码已存在";
    private static final String DELETE_PUBLISHED = "请先下线专题后再删除";
    private static final String PUBLISH_INCOMPLETE = "发布专题缺少必要信息";
    private static final String PUBLISH_NO_CONTENT = "发布专题至少需要一篇已发布的内容";
    private static final String CONTENT_TYPE_UNSUPPORTED = "专题只支持文章、视频、题目、每周一题和资料";
    private static final String CONTENT_NOT_FOUND = "内容不存在";
    private static final String RELATION_NOT_FOUND = "专题内容不存在";
    private static final String ITEM_OTHER_TOPIC = "专题内容不属于当前专题";
    private static final String ITEM_EXISTS = "专题内容已存在";
    private static final String DUPLICATE_SORT_IDS = "排序项不能包含重复内容";
    private static final int MAX_ADMIN_CONTENTS = 100;
    private static final int MAX_FEATURED = 4;

    private final TopicMapper topicMapper;
    private final TopicContentMapper topicContentMapper;
    private final ContentMapper contentMapper;
    private final CategoryService categoryService;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<PublicTopicListVO> publicPage(@Valid TopicQueryRequest request) {
        IPage<Topic> result = queryTopicPage(request, true);
        List<Topic> topics = result.getRecords();
        if (topics.isEmpty()) {
            return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), List.of());
        }
        Map<Long, String> categoryNames = categoryService.namesByIds(
                topics.stream().map(Topic::getCategoryId).toList());
        List<PublicTopicListVO> records = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            PublicTopicListVO vo = new PublicTopicListVO();
            vo.setId(topic.getId());
            vo.setName(topic.getName());
            vo.setCoverUrl(topic.getCoverUrl());
            vo.setSummary(topic.getSummary());
            vo.setCategoryId(topic.getCategoryId());
            vo.setCategoryName(categoryNames.get(topic.getCategoryId()));
            vo.setSort(topic.getSort());
            vo.setPublishTime(topic.getPublishTime());
            records.add(vo);
        }
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    public List<PublicTopicListVO> publicFeatured(Integer pageSize) {
        int size = pageSize == null ? MAX_FEATURED : pageSize;
        if (size < 1) {
            size = 1;
        }
        if (size > MAX_FEATURED) {
            size = MAX_FEATURED;
        }
        TopicQueryRequest request = new TopicQueryRequest();
        request.setPageNum(1);
        request.setPageSize(size);
        return publicPage(request).getRecords();
    }

    public PublicTopicDetailVO publicDetail(Long id) {
        Topic topic = requireTopic(id);
        if (topic.getStatus() != TopicStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        PublicTopicDetailVO detail = new PublicTopicDetailVO();
        detail.setId(topic.getId());
        detail.setName(topic.getName());
        detail.setCoverUrl(topic.getCoverUrl());
        detail.setSummary(topic.getSummary());
        PublicTopicCategoryVO category = new PublicTopicCategoryVO();
        category.setId(topic.getCategoryId());
        Map<Long, String> names = categoryService.namesByIds(List.of(topic.getCategoryId()));
        category.setName(names.get(topic.getCategoryId()));
        detail.setCategory(category);

        List<TopicContentRow> rows = topicContentMapper.selectPublishedContents(id);
        List<PublicTopicContentVO> contents = new ArrayList<>(rows.size());
        for (TopicContentRow row : rows) {
            PublicTopicContentVO item = new PublicTopicContentVO();
            item.setId(row.getContentId());
            item.setTitle(row.getTitle());
            item.setContentType(row.getContentType());
            item.setCoverUrl(row.getCoverUrl());
            item.setSummary(row.getSummary());
            item.setPublishTime(row.getPublishTime());
            contents.add(item);
        }
        detail.setContents(contents);
        return detail;
    }

    @RequirePermission(PermissionCodes.TOPIC_VIEW)
    public PageResult<TopicListVO> adminPage(@Valid TopicQueryRequest request) {
        IPage<Topic> result = queryTopicPage(request, false);
        List<Topic> topics = result.getRecords();
        if (topics.isEmpty()) {
            return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), List.of());
        }
        List<Long> topicIds = topics.stream().map(Topic::getId).toList();
        Map<Long, String> categoryNames = categoryService.namesByIds(
                topics.stream().map(Topic::getCategoryId).toList());
        Map<Long, Long> counts = contentCounts(topicIds);
        List<TopicListVO> records = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            TopicListVO vo = toList(topic);
            vo.setCategoryName(categoryNames.get(topic.getCategoryId()));
            vo.setContentCount(counts.getOrDefault(topic.getId(), 0L).intValue());
            records.add(vo);
        }
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @RequirePermission(PermissionCodes.TOPIC_VIEW)
    public TopicDetailVO adminDetail(Long id) {
        return detailOf(requireTopic(id));
    }

    @RequirePermission(PermissionCodes.TOPIC_CREATE)
    @Transactional
    public TopicDetailVO create(@Valid TopicCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        String code = request.getCode().trim();
        assertCodeAvailable(code);

        Topic topic = new Topic();
        topic.setName(request.getName().trim());
        topic.setCode(code);
        topic.setCoverUrl(TextValues.trimToNull(request.getCoverUrl()));
        topic.setSummary(TextValues.trimToNull(request.getSummary()));
        topic.setCategoryId(request.getCategoryId());
        topic.setSort(request.getSort());
        topic.setStatus(TopicStatus.DRAFT);
        topic.setPublishTime(null);
        try {
            topicMapper.insert(topic);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_CODE);
        }
        BizLogger.info("topic.create", "id={} code={}", topic.getId(), topic.getCode());
        return detailOf(requireTopic(topic.getId()));
    }

    @RequirePermission(PermissionCodes.TOPIC_UPDATE)
    @Transactional
    public TopicDetailVO update(Long id, @Valid TopicUpdateRequest request) {
        Topic topic = requireTopic(id);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        topic.setName(request.getName().trim());
        topic.setCoverUrl(TextValues.trimToNull(request.getCoverUrl()));
        topic.setSummary(TextValues.trimToNull(request.getSummary()));
        topic.setCategoryId(request.getCategoryId());
        topic.setSort(request.getSort());
        topicMapper.updateById(topic);
        BizLogger.info("topic.update", "id={}", id);
        return detailOf(requireTopic(id));
    }

    @RequirePermission(PermissionCodes.TOPIC_DELETE)
    @Transactional
    public void delete(Long id) {
        Topic topic = requireTopic(id);
        if (topic.getStatus() == TopicStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DELETE_PUBLISHED);
        }
        topicContentMapper.delete(Wrappers.<TopicContent>lambdaQuery().eq(TopicContent::getTopicId, id));
        topicMapper.deleteById(id);
        BizLogger.info("topic.delete", "id={}", id);
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    public List<TopicContentItemVO> listContents(Long topicId) {
        requireTopic(topicId);
        return loadContents(topicId);
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public List<TopicContentItemVO> addContents(Long topicId, @Valid TopicContentAddRequest request) {
        requireTopic(topicId);
        List<Long> contentIds = normalizeContentIds(request.getContentIds());
        List<Content> contents = contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getContentType, Content::getStatus)
                .in(Content::getId, contentIds));
        Map<Long, Content> byId = new HashMap<>();
        for (Content content : contents) {
            byId.put(content.getId(), content);
        }
        for (Long contentId : contentIds) {
            Content content = byId.get(contentId);
            if (content == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, CONTENT_NOT_FOUND);
            }
            if (content.getContentType() != ContentType.ARTICLE
                    && content.getContentType() != ContentType.VIDEO
                    && content.getContentType() != ContentType.QUESTION
                    && content.getContentType() != ContentType.WEEKLY
                    && content.getContentType() != ContentType.DOCUMENT) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, CONTENT_TYPE_UNSUPPORTED);
            }
        }

        Integer maxSort = topicContentMapper.selectMaxSort(topicId);
        int nextSort = maxSort == null ? 0 : maxSort + 1;
        for (Long contentId : contentIds) {
            if (insertIfAbsent(topicId, contentId, nextSort)) {
                nextSort++;
            }
        }
        BizLogger.info("topic.addContents", "topicId={} count={}", topicId, contentIds.size());
        return loadContents(topicId);
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public TopicContentItemVO addItem(Long topicId, @Valid TopicItemAddRequest request) {
        requireTopic(topicId);
        Content content = requireSupportedContent(request.getContentId());
        int sort = request.getSort() == null ? nextSort(topicId) : request.getSort();
        if (!insertIfAbsent(topicId, content.getId(), sort)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ITEM_EXISTS);
        }
        BizLogger.info("topic.addItem", "topicId={} contentId={}", topicId, content.getId());
        return findItem(topicId, content.getId());
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public TopicItemBatchResultVO addItems(Long topicId, @Valid TopicItemBatchRequest request) {
        requireTopic(topicId);
        List<Long> contentIds = request.getContentIds().stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, Content> byId = loadContentsByIds(contentIds);
        TopicItemBatchResultVO result = new TopicItemBatchResultVO();
        int nextSort = nextSort(topicId);
        for (Long contentId : contentIds) {
            Content content = byId.get(contentId);
            if (!supported(content)) {
                result.setInvalidCount(result.getInvalidCount() + 1);
                continue;
            }
            if (!insertIfAbsent(topicId, contentId, nextSort)) {
                result.setDuplicateCount(result.getDuplicateCount() + 1);
                continue;
            }
            result.setSuccessCount(result.getSuccessCount() + 1);
            nextSort++;
        }
        BizLogger.info("topic.addItems", "topicId={} success={} duplicate={} invalid={}",
                topicId, result.getSuccessCount(), result.getDuplicateCount(), result.getInvalidCount());
        return result;
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public void removeItem(Long topicId, Long itemId) {
        requireTopic(topicId);
        TopicContent relation = topicContentMapper.selectOne(Wrappers.<TopicContent>lambdaQuery()
                .select(TopicContent::getId, TopicContent::getTopicId, TopicContent::getContentId)
                .eq(TopicContent::getId, itemId));
        if (relation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, RELATION_NOT_FOUND);
        }
        if (!Objects.equals(relation.getTopicId(), topicId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ITEM_OTHER_TOPIC);
        }
        topicContentMapper.deleteById(relation.getId());
        BizLogger.info("topic.removeItem", "topicId={} itemId={} contentId={}",
                topicId, itemId, relation.getContentId());
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public List<TopicContentItemVO> sortItems(Long topicId, @Valid TopicItemSortRequest request) {
        requireTopic(topicId);
        List<TopicItemSortRequest.Item> items = request.getItems();
        Set<Long> seen = new HashSet<>();
        List<Long> itemIds = new ArrayList<>(items.size());
        for (TopicItemSortRequest.Item item : items) {
            if (!seen.add(item.getItemId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_SORT_IDS);
            }
            itemIds.add(item.getItemId());
        }
        List<TopicContent> relations = topicContentMapper.selectList(Wrappers.<TopicContent>lambdaQuery()
                .select(TopicContent::getId, TopicContent::getTopicId, TopicContent::getSort)
                .in(TopicContent::getId, itemIds));
        Map<Long, TopicContent> byId = new HashMap<>();
        for (TopicContent relation : relations) {
            byId.put(relation.getId(), relation);
        }
        for (TopicItemSortRequest.Item item : items) {
            TopicContent relation = byId.get(item.getItemId());
            if (relation == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, RELATION_NOT_FOUND);
            }
            if (!Objects.equals(relation.getTopicId(), topicId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ITEM_OTHER_TOPIC);
            }
            if (!Objects.equals(relation.getSort(), item.getSort())) {
                relation.setSort(item.getSort());
                topicContentMapper.updateById(relation);
            }
        }
        BizLogger.info("topic.sortItems", "topicId={} count={}", topicId, items.size());
        return loadContents(topicId);
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public void removeContent(Long topicId, Long contentId) {
        requireTopic(topicId);
        TopicContent relation = topicContentMapper.selectOne(Wrappers.<TopicContent>lambdaQuery()
                .select(TopicContent::getId)
                .eq(TopicContent::getTopicId, topicId)
                .eq(TopicContent::getContentId, contentId));
        if (relation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, RELATION_NOT_FOUND);
        }
        topicContentMapper.deleteById(relation.getId());
        BizLogger.info("topic.removeContent", "topicId={} contentId={}", topicId, contentId);
    }

    @RequirePermission(PermissionCodes.TOPIC_CONTENT_MANAGE)
    @Transactional
    public List<TopicContentItemVO> sortContents(Long topicId, @Valid TopicContentSortRequest request) {
        requireTopic(topicId);
        List<TopicContentSortRequest.Item> items = request.getItems();
        Set<Long> seen = new HashSet<>();
        List<Long> contentIds = new ArrayList<>(items.size());
        for (TopicContentSortRequest.Item item : items) {
            if (!seen.add(item.getContentId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_SORT_IDS);
            }
            contentIds.add(item.getContentId());
        }
        List<TopicContent> relations = topicContentMapper.selectList(Wrappers.<TopicContent>lambdaQuery()
                .select(TopicContent::getId, TopicContent::getContentId, TopicContent::getSort)
                .eq(TopicContent::getTopicId, topicId)
                .in(TopicContent::getContentId, contentIds));
        Map<Long, TopicContent> byContentId = new HashMap<>();
        for (TopicContent relation : relations) {
            byContentId.put(relation.getContentId(), relation);
        }
        for (TopicContentSortRequest.Item item : items) {
            TopicContent relation = byContentId.get(item.getContentId());
            if (relation == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, RELATION_NOT_FOUND);
            }
            if (!Objects.equals(relation.getSort(), item.getSort())) {
                relation.setSort(item.getSort());
                topicContentMapper.updateById(relation);
            }
        }
        BizLogger.info("topic.sortContents", "topicId={} count={}", topicId, items.size());
        return loadContents(topicId);
    }

    @RequirePermission(PermissionCodes.TOPIC_PUBLISH)
    @Transactional
    public TopicDetailVO publish(Long id) {
        Topic topic = requireTopic(id);
        assertPublishable(topic);
        if (topic.getStatus() != TopicStatus.PUBLISHED) {
            topic.setStatus(TopicStatus.PUBLISHED);
            topic.setPublishTime(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
            topicMapper.updateById(topic);
        }
        BizLogger.info("topic.publish", "id={} status={}", id, topic.getStatus());
        return detailOf(topic);
    }

    @RequirePermission(PermissionCodes.TOPIC_OFFLINE)
    @Transactional
    public TopicDetailVO offline(Long id) {
        Topic topic = requireTopic(id);
        if (topic.getStatus() != TopicStatus.OFFLINE) {
            topic.setStatus(TopicStatus.OFFLINE);
            topicMapper.updateById(topic);
        }
        BizLogger.info("topic.offline", "id={} status={}", id, topic.getStatus());
        return detailOf(topic);
    }

    private IPage<Topic> queryTopicPage(TopicQueryRequest request, boolean publicQuery) {
        List<Long> categoryIds = null;
        if (request.getCategoryId() != null) {
            categoryIds = categoryService.selfAndDescendantIds(request.getCategoryId());
            if (categoryIds.isEmpty()) {
                return new Page<>(request.getPageNum(), request.getPageSize(), 0);
            }
        }
        TopicPageQuery query = new TopicPageQuery(
                categoryIds,
                publicQuery || request.getStatus() == null ? null : request.getStatus().getCode(),
                ContentService.likePattern(request.getKeyword()),
                publicQuery);
        return topicMapper.selectTopicPage(new Page<>(request.getPageNum(), request.getPageSize()), query);
    }

    private Topic requireTopic(Long id) {
        Topic topic = topicMapper.selectOne(Wrappers.<Topic>lambdaQuery()
                .select(Topic::getId, Topic::getName, Topic::getCode, Topic::getCoverUrl, Topic::getSummary,
                        Topic::getCategoryId, Topic::getStatus, Topic::getSort, Topic::getPublishTime,
                        Topic::getCreatedAt, Topic::getUpdatedAt)
                .eq(Topic::getId, id));
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return topic;
    }

    private void assertCodeAvailable(String code) {
        Long count = topicMapper.selectCount(Wrappers.<Topic>lambdaQuery().eq(Topic::getCode, code));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_CODE);
        }
    }

    private void assertPublishable(Topic topic) {
        if (topic.getName() == null || topic.getName().isBlank() || topic.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PUBLISH_INCOMPLETE);
        }
        categoryService.assertContentCategory(topic.getCategoryId(), true);
        long published = topicMapper.countPublishedContents(topic.getId());
        if (published < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PUBLISH_NO_CONTENT);
        }
    }

    private Map<Long, Long> contentCounts(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return Map.of();
        }
        List<TopicContentCountRow> rows = topicMapper.countContentsByTopicIds(topicIds);
        Map<Long, Long> counts = new HashMap<>();
        for (TopicContentCountRow row : rows) {
            counts.put(row.getTopicId(), row.getContentCount() == null ? 0L : row.getContentCount());
        }
        return counts;
    }

    private Content requireSupportedContent(Long contentId) {
        Content content = contentMapper.selectOne(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getContentType, Content::getStatus)
                .eq(Content::getId, contentId));
        if (!supported(content)) {
            if (content == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, CONTENT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, CONTENT_TYPE_UNSUPPORTED);
        }
        return content;
    }

    private Map<Long, Content> loadContentsByIds(List<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        List<Content> contents = contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getContentType, Content::getStatus)
                .in(Content::getId, contentIds));
        Map<Long, Content> byId = new HashMap<>();
        for (Content content : contents) {
            byId.put(content.getId(), content);
        }
        return byId;
    }

    private static boolean supported(Content content) {
        if (content == null || content.getContentType() == null) {
            return false;
        }
        ContentType type = content.getContentType();
        return type == ContentType.ARTICLE
                || type == ContentType.VIDEO
                || type == ContentType.QUESTION
                || type == ContentType.WEEKLY
                || type == ContentType.DOCUMENT;
    }

    private int nextSort(Long topicId) {
        Integer maxSort = topicContentMapper.selectMaxSort(topicId);
        return maxSort == null ? 0 : maxSort + 1;
    }

    private boolean insertIfAbsent(Long topicId, Long contentId, int sort) {
        Integer active = jdbcTemplate.query(
                """
                        SELECT deleted FROM topic_content
                        WHERE topic_id = ? AND content_id = ? AND deleted = 0
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getInt(1) : null,
                topicId, contentId);
        if (active != null) {
            return false;
        }
        jdbcTemplate.update(
                "DELETE FROM topic_content WHERE topic_id = ? AND content_id = ? AND deleted <> 0",
                topicId, contentId);
        TopicContent relation = new TopicContent();
        relation.setTopicId(topicId);
        relation.setContentId(contentId);
        relation.setSort(sort);
        topicContentMapper.insert(relation);
        return true;
    }

    private TopicContentItemVO findItem(Long topicId, Long contentId) {
        for (TopicContentItemVO item : loadContents(topicId)) {
            if (Objects.equals(item.getContentId(), contentId)) {
                return item;
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, RELATION_NOT_FOUND);
    }

    private static List<Long> normalizeContentIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容 ID 不能为空");
        }
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容 ID 不能为空");
        }
        if (distinct.size() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次最多添加 100 条内容");
        }
        return distinct;
    }

    private TopicDetailVO detailOf(Topic topic) {
        TopicDetailVO detail = toDetail(topic);
        Map<Long, Long> counts = contentCounts(List.of(topic.getId()));
        detail.setContentCount(counts.getOrDefault(topic.getId(), 0L).intValue());
        Map<Long, String> names = categoryService.namesByIds(List.of(topic.getCategoryId()));
        detail.setCategoryName(names.get(topic.getCategoryId()));
        return detail;
    }

    private List<TopicContentItemVO> loadContents(Long topicId) {
        List<TopicContentRow> rows = topicContentMapper.selectAdminContents(topicId, MAX_ADMIN_CONTENTS);
        Map<Long, String> names = categoryService.namesByIds(
                rows.stream().map(TopicContentRow::getCategoryId).toList());
        List<TopicContentItemVO> items = new ArrayList<>(rows.size());
        for (TopicContentRow row : rows) {
            TopicContentItemVO item = new TopicContentItemVO();
            item.setId(row.getItemId());
            item.setContentId(row.getContentId());
            item.setTitle(row.getTitle());
            item.setContentType(row.getContentType());
            item.setCoverUrl(row.getCoverUrl());
            item.setCategoryId(row.getCategoryId());
            item.setCategoryName(names.get(row.getCategoryId()));
            item.setStatus(row.getStatus());
            item.setSort(row.getSort());
            items.add(item);
        }
        return items;
    }

    private static TopicListVO toList(Topic topic) {
        TopicListVO vo = new TopicListVO();
        vo.setId(topic.getId());
        vo.setName(topic.getName());
        vo.setCode(topic.getCode());
        vo.setCoverUrl(topic.getCoverUrl());
        vo.setSummary(topic.getSummary());
        vo.setCategoryId(topic.getCategoryId());
        vo.setStatus(topic.getStatus());
        vo.setSort(topic.getSort());
        vo.setPublishTime(topic.getPublishTime());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
        return vo;
    }

    private static TopicDetailVO toDetail(Topic topic) {
        TopicDetailVO vo = new TopicDetailVO();
        vo.setId(topic.getId());
        vo.setName(topic.getName());
        vo.setCode(topic.getCode());
        vo.setCoverUrl(topic.getCoverUrl());
        vo.setSummary(topic.getSummary());
        vo.setCategoryId(topic.getCategoryId());
        vo.setStatus(topic.getStatus());
        vo.setSort(topic.getSort());
        vo.setPublishTime(topic.getPublishTime());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
        return vo;
    }
}
