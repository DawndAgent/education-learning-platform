package com.xxedu.learning.modules.home.banner.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.HttpUrls;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.home.banner.dto.BannerQueryRequest;
import com.xxedu.learning.modules.home.banner.dto.BannerSaveRequest;
import com.xxedu.learning.modules.home.banner.entity.HomeBanner;
import com.xxedu.learning.modules.home.banner.mapper.HomeBannerMapper;
import com.xxedu.learning.modules.home.banner.vo.BannerAdminVO;
import com.xxedu.learning.modules.home.dto.HomeSortRequest;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import com.xxedu.learning.modules.topic.entity.Topic;
import com.xxedu.learning.modules.topic.mapper.TopicMapper;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
@RequiredArgsConstructor
public class HomeBannerService {

    private static final String NOT_FOUND = "Banner 不存在";

    private final HomeBannerMapper bannerMapper;
    private final ContentMapper contentMapper;
    private final TopicMapper topicMapper;

    @RequirePermission(PermissionCodes.HOME_OPERATION_VIEW)
    public PageResult<BannerAdminVO> page(@Valid BannerQueryRequest request) {
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        Page<HomeBanner> result = bannerMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                Wrappers.<HomeBanner>lambdaQuery()
                        .select(HomeBanner::getId, HomeBanner::getTitle, HomeBanner::getSubtitle,
                                HomeBanner::getImageUrl, HomeBanner::getLinkType, HomeBanner::getLinkId,
                                HomeBanner::getLinkUrl, HomeBanner::getSort, HomeBanner::getStatus,
                                HomeBanner::getStartTime, HomeBanner::getEndTime)
                        .like(keyword != null && !keyword.isEmpty(), HomeBanner::getTitle, keyword)
                        .eq(request.getStatus() != null, HomeBanner::getStatus, request.getStatus())
                        .orderByAsc(HomeBanner::getSort)
                        .orderByDesc(HomeBanner::getId));
        List<BannerAdminVO> records = result.getRecords().stream().map(this::toVo).toList();
        fillTargetTitles(records);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public BannerAdminVO create(@Valid BannerSaveRequest request) {
        Link link = resolveLink(request);
        HomeBanner banner = new HomeBanner();
        apply(banner, request, link);
        banner.setStatus(HomeItemStatus.DISABLED);
        bannerMapper.insert(banner);
        BizLogger.info("home.banner.create", "id={}", banner.getId());
        return toVo(require(banner.getId()));
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public BannerAdminVO update(Long id, @Valid BannerSaveRequest request) {
        require(id);
        Link link = resolveLink(request);
        bannerMapper.update(null, Wrappers.<HomeBanner>lambdaUpdate()
                .set(HomeBanner::getTitle, request.getTitle().trim())
                .set(HomeBanner::getSubtitle, TextValues.trimToNull(request.getSubtitle()))
                .set(HomeBanner::getImageUrl, request.getImageUrl().trim())
                .set(HomeBanner::getLinkType, link.type())
                .set(HomeBanner::getLinkId, link.id())
                .set(HomeBanner::getLinkUrl, link.url())
                .set(HomeBanner::getSort, request.getSort())
                .set(HomeBanner::getStartTime, request.getStartTime())
                .set(HomeBanner::getEndTime, request.getEndTime())
                .eq(HomeBanner::getId, id));
        BizLogger.info("home.banner.update", "id={}", id);
        return toVo(require(id));
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public void delete(Long id) {
        require(id);
        bannerMapper.update(null, Wrappers.<HomeBanner>lambdaUpdate()
                .set(HomeBanner::getStatus, HomeItemStatus.DISABLED)
                .eq(HomeBanner::getId, id));
        bannerMapper.deleteById(id);
        BizLogger.info("home.banner.delete", "id={}", id);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public BannerAdminVO enable(Long id) {
        return changeStatus(id, HomeItemStatus.ENABLED);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public BannerAdminVO disable(Long id) {
        return changeStatus(id, HomeItemStatus.DISABLED);
    }

    @RequirePermission(PermissionCodes.HOME_OPERATION_MANAGE)
    @Transactional
    public void sort(@Valid HomeSortRequest request) {
        for (HomeSortRequest.Item item : request.getItems()) {
            bannerMapper.update(null, Wrappers.<HomeBanner>lambdaUpdate()
                    .set(HomeBanner::getSort, item.getSort())
                    .eq(HomeBanner::getId, item.getId()));
        }
        BizLogger.info("home.banner.sort", "count={}", request.getItems().size());
    }

    private BannerAdminVO changeStatus(Long id, HomeItemStatus status) {
        require(id);
        bannerMapper.update(null, Wrappers.<HomeBanner>lambdaUpdate()
                .set(HomeBanner::getStatus, status)
                .eq(HomeBanner::getId, id));
        BizLogger.info("home.banner.status", "id={} status={}", id, status);
        return toVo(require(id));
    }

    private HomeBanner require(Long id) {
        HomeBanner banner = bannerMapper.selectOne(Wrappers.<HomeBanner>lambdaQuery()
                .select(HomeBanner::getId, HomeBanner::getTitle, HomeBanner::getSubtitle,
                        HomeBanner::getImageUrl, HomeBanner::getLinkType, HomeBanner::getLinkId,
                        HomeBanner::getLinkUrl, HomeBanner::getSort, HomeBanner::getStatus,
                        HomeBanner::getStartTime, HomeBanner::getEndTime)
                .eq(HomeBanner::getId, id));
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return banner;
    }

    private void apply(HomeBanner banner, BannerSaveRequest request, Link link) {
        banner.setTitle(request.getTitle().trim());
        banner.setSubtitle(TextValues.trimToNull(request.getSubtitle()));
        banner.setImageUrl(request.getImageUrl().trim());
        banner.setLinkType(link.type());
        banner.setLinkId(link.id());
        banner.setLinkUrl(link.url());
        banner.setSort(request.getSort());
        banner.setStartTime(request.getStartTime());
        banner.setEndTime(request.getEndTime());
    }

    private Link resolveLink(BannerSaveRequest request) {
        if (!HttpUrls.isMediaUrl(request.getImageUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不合法");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
        HomeLinkType type = request.getLinkType();
        if (type == HomeLinkType.CONTENT) {
            if (request.getLinkId() == null || !contentExists(request.getLinkId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "跳转内容不存在");
            }
            return new Link(type, request.getLinkId(), null);
        }
        if (type == HomeLinkType.TOPIC) {
            if (request.getLinkId() == null || !topicExists(request.getLinkId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "跳转专题不存在");
            }
            return new Link(type, request.getLinkId(), null);
        }
        if (type == HomeLinkType.URL) {
            String url = TextValues.trimToNull(request.getLinkUrl());
            if (url == null || !HttpUrls.isHttp(url)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "跳转链接不合法");
            }
            return new Link(type, null, url);
        }
        return new Link(HomeLinkType.NONE, null, null);
    }

    private void fillTargetTitles(List<BannerAdminVO> records) {
        List<Long> contentIds = records.stream()
                .filter(item -> item.getLinkType() == HomeLinkType.CONTENT && item.getLinkId() != null)
                .map(BannerAdminVO::getLinkId).distinct().toList();
        List<Long> topicIds = records.stream()
                .filter(item -> item.getLinkType() == HomeLinkType.TOPIC && item.getLinkId() != null)
                .map(BannerAdminVO::getLinkId).distinct().toList();
        Map<Long, String> titles = new HashMap<>();
        if (!contentIds.isEmpty()) {
            contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                            .select(Content::getId, Content::getTitle)
                            .in(Content::getId, contentIds))
                    .forEach(item -> titles.put(item.getId(), item.getTitle()));
        }
        if (!topicIds.isEmpty()) {
            topicMapper.selectList(Wrappers.<Topic>lambdaQuery()
                            .select(Topic::getId, Topic::getName)
                            .in(Topic::getId, topicIds))
                    .forEach(item -> titles.put(item.getId(), item.getName()));
        }
        for (BannerAdminVO record : records) {
            if (record.getLinkType() == HomeLinkType.URL) {
                record.setTargetTitle(record.getLinkUrl());
            } else if (record.getLinkId() != null) {
                record.setTargetTitle(titles.get(record.getLinkId()));
            }
        }
    }

    private BannerAdminVO toVo(HomeBanner banner) {
        BannerAdminVO vo = new BannerAdminVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setSubtitle(banner.getSubtitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkType(banner.getLinkType());
        vo.setLinkId(banner.getLinkId());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setSort(banner.getSort());
        vo.setStatus(banner.getStatus());
        vo.setStartTime(banner.getStartTime());
        vo.setEndTime(banner.getEndTime());
        return vo;
    }

    private boolean contentExists(Long id) {
        return contentMapper.selectOne(Wrappers.<Content>lambdaQuery()
                .select(Content::getId)
                .eq(Content::getId, id)) != null;
    }

    private boolean topicExists(Long id) {
        return topicMapper.selectOne(Wrappers.<Topic>lambdaQuery()
                .select(Topic::getId)
                .eq(Topic::getId, id)) != null;
    }

    private record Link(HomeLinkType type, Long id, String url) {
    }
}
