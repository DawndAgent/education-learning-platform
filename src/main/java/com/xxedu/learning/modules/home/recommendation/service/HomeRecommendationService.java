package com.xxedu.learning.modules.home.recommendation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.home.dto.HomeSortRequest;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.RecommendType;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationCreateRequest;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationQueryRequest;
import com.xxedu.learning.modules.home.recommendation.entity.HomeRecommendation;
import com.xxedu.learning.modules.home.recommendation.mapper.HomeRecommendationMapper;
import com.xxedu.learning.modules.home.recommendation.vo.RecommendationAdminVO;
import com.xxedu.learning.modules.topic.entity.Topic;
import com.xxedu.learning.modules.topic.mapper.TopicMapper;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
@RequiredArgsConstructor
public class HomeRecommendationService {

    private static final String NOT_FOUND = "推荐不存在";
    private static final String DUPLICATE = "该目标已在重点推荐中";

    private final HomeRecommendationMapper recommendationMapper;
    private final ContentMapper contentMapper;
    private final TopicMapper topicMapper;
    private final CategoryService categoryService;
    private final JdbcTemplate jdbcTemplate;

    @RequirePermission(PermissionCodes.HOME_OPERATION_VIEW)
    public PageResult<RecommendationAdminVO> page(@Valid RecommendationQueryRequest request) {
        Page<HomeRecommendation> result = recommendationMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                Wrappers.<HomeRecommendation>lambdaQuery()
                        .select(HomeRecommendation::getId, HomeRecommendation::getTitle,
                                HomeRecommendation::getRecommendType, HomeRecommendation::getTargetId,
                                HomeRecommendation::getSort, HomeRecommendation::getStatus)
                        .eq(request.getRecommendType() != null, HomeRecommendation::getRecommendType, request.getRecommendType())
                        .eq(request.getStatus() != null, HomeRecommendation::getStatus, request.getStatus())
                        .orderByAsc(HomeRecommendation::getSort)
                        .orderByDesc(HomeRecommendation::getId));
        List<RecommendationAdminVO> records = result.getRecords().stream().map(this::toVo).toList();
        fillTargets(records);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public RecommendationAdminVO create(@Valid RecommendationCreateRequest request) {
        Target target = requireTarget(request.getRecommendType(), request.getTargetId());
        HomeRecommendation existing = recommendationMapper.selectOne(Wrappers.<HomeRecommendation>lambdaQuery()
                .select(HomeRecommendation::getId)
                .eq(HomeRecommendation::getRecommendType, request.getRecommendType())
                .eq(HomeRecommendation::getTargetId, request.getTargetId()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE);
        }
        int sort = request.getSort() == null ? 0 : request.getSort();
        Long restoredId = restoreDeleted(request.getRecommendType(), request.getTargetId(), target.title(), sort);
        if (restoredId != null) {
            BizLogger.info("home.recommendation.create", "id={} type={} restored=true", restoredId, request.getRecommendType());
            return detail(restoredId);
        }
        HomeRecommendation row = new HomeRecommendation();
        row.setRecommendType(request.getRecommendType());
        row.setTargetId(request.getTargetId());
        row.setTitle(target.title());
        row.setSort(sort);
        row.setStatus(HomeItemStatus.ENABLED);
        recommendationMapper.insert(row);
        BizLogger.info("home.recommendation.create", "id={} type={}", row.getId(), row.getRecommendType());
        return detail(row.getId());
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public void delete(Long id) {
        HomeRecommendation row = recommendationMapper.selectOne(Wrappers.<HomeRecommendation>lambdaQuery()
                .select(HomeRecommendation::getId)
                .eq(HomeRecommendation::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        recommendationMapper.update(null, Wrappers.<HomeRecommendation>lambdaUpdate()
                .set(HomeRecommendation::getStatus, HomeItemStatus.DISABLED)
                .eq(HomeRecommendation::getId, id));
        recommendationMapper.deleteById(id);
        BizLogger.info("home.recommendation.delete", "id={}", id);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public void sort(@Valid HomeSortRequest request) {
        for (HomeSortRequest.Item item : request.getItems()) {
            recommendationMapper.update(null, Wrappers.<HomeRecommendation>lambdaUpdate()
                    .set(HomeRecommendation::getSort, item.getSort())
                    .eq(HomeRecommendation::getId, item.getId()));
        }
        BizLogger.info("home.recommendation.sort", "count={}", request.getItems().size());
    }

    private Long restoreDeleted(RecommendType type, Long targetId, String title, int sort) {
        Long tombstoneId = jdbcTemplate.query(
                """
                        SELECT id FROM home_recommendation
                        WHERE recommend_type = ? AND target_id = ? AND deleted = 1
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong(1) : null,
                type.getCode(), targetId);
        if (tombstoneId == null) {
            return null;
        }
        int updated = jdbcTemplate.update(
                """
                        UPDATE home_recommendation
                        SET deleted = 0, status = 'ENABLED', title = ?, sort = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND deleted = 1
                        """,
                title, sort, tombstoneId);
        return updated == 0 ? null : tombstoneId;
    }

    private RecommendationAdminVO detail(Long id) {
        HomeRecommendation row = recommendationMapper.selectOne(Wrappers.<HomeRecommendation>lambdaQuery()
                .select(HomeRecommendation::getId, HomeRecommendation::getTitle,
                        HomeRecommendation::getRecommendType, HomeRecommendation::getTargetId,
                        HomeRecommendation::getSort, HomeRecommendation::getStatus)
                .eq(HomeRecommendation::getId, id));
        RecommendationAdminVO vo = toVo(row);
        fillTargets(List.of(vo));
        return vo;
    }

    private Target requireTarget(RecommendType type, Long targetId) {
        if (type == RecommendType.CONTENT) {
            Content content = contentMapper.selectOne(Wrappers.<Content>lambdaQuery()
                    .select(Content::getId, Content::getTitle)
                    .eq(Content::getId, targetId));
            if (content == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "推荐内容不存在");
            }
            return new Target(content.getTitle());
        }
        Topic topic = topicMapper.selectOne(Wrappers.<Topic>lambdaQuery()
                .select(Topic::getId, Topic::getName)
                .eq(Topic::getId, targetId));
        if (topic == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "推荐专题不存在");
        }
        return new Target(topic.getName());
    }

    private void fillTargets(List<RecommendationAdminVO> records) {
        List<Long> contentIds = records.stream()
                .filter(item -> item.getRecommendType() == RecommendType.CONTENT)
                .map(RecommendationAdminVO::getTargetId).distinct().toList();
        List<Long> topicIds = records.stream()
                .filter(item -> item.getRecommendType() == RecommendType.TOPIC)
                .map(RecommendationAdminVO::getTargetId).distinct().toList();
        Map<Long, Content> contents = new HashMap<>();
        Map<Long, Topic> topics = new HashMap<>();
        if (!contentIds.isEmpty()) {
            contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                            .select(Content::getId, Content::getTitle, Content::getCoverUrl, Content::getCategoryId,
                                    Content::getContentType, Content::getStatus)
                            .in(Content::getId, contentIds))
                    .forEach(item -> contents.put(item.getId(), item));
        }
        if (!topicIds.isEmpty()) {
            topicMapper.selectList(Wrappers.<Topic>lambdaQuery()
                            .select(Topic::getId, Topic::getName, Topic::getCoverUrl, Topic::getCategoryId, Topic::getStatus)
                            .in(Topic::getId, topicIds))
                    .forEach(item -> topics.put(item.getId(), item));
        }
        List<Long> categoryIds = new java.util.ArrayList<>();
        contents.values().forEach(item -> categoryIds.add(item.getCategoryId()));
        topics.values().forEach(item -> categoryIds.add(item.getCategoryId()));
        Map<Long, String> names = categoryService.namesByIds(categoryIds);
        for (RecommendationAdminVO record : records) {
            if (record.getRecommendType() == RecommendType.CONTENT) {
                Content content = contents.get(record.getTargetId());
                if (content == null) {
                    continue;
                }
                record.setTitle(content.getTitle());
                record.setCoverUrl(content.getCoverUrl());
                record.setContentType(content.getContentType());
                record.setContentStatus(content.getStatus());
                record.setCategoryName(names.get(content.getCategoryId()));
            } else {
                Topic topic = topics.get(record.getTargetId());
                if (topic == null) {
                    continue;
                }
                record.setTitle(topic.getName());
                record.setCoverUrl(topic.getCoverUrl());
                record.setTopicStatus(topic.getStatus());
                record.setCategoryName(names.get(topic.getCategoryId()));
            }
        }
    }

    private RecommendationAdminVO toVo(HomeRecommendation row) {
        RecommendationAdminVO vo = new RecommendationAdminVO();
        vo.setId(row.getId());
        vo.setRecommendType(row.getRecommendType());
        vo.setTargetId(row.getTargetId());
        vo.setTitle(row.getTitle());
        vo.setSort(row.getSort());
        vo.setStatus(row.getStatus());
        return vo;
    }

    private record Target(String title) {
    }
}
