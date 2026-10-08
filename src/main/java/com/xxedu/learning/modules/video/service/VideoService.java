package com.xxedu.learning.modules.video.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.HttpUrls;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.video.convert.VideoConverter;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.dto.VideoUpdateRequest;
import com.xxedu.learning.modules.video.entity.Video;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.mapper.VideoMapper;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
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
public class VideoService {

    private static final String NOT_FOUND = "视频不存在";

    private final ContentMapper contentMapper;
    private final VideoMapper videoMapper;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final VideoConverter videoConverter;

    @RequirePermission(PermissionCodes.VIDEO_MANAGE)
    @Transactional
    public VideoDetailVO create(@Valid VideoCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = newContent(request);
        contentMapper.insert(content);
        Video video = new Video();
        video.setContentId(content.getId());
        applyVideo(video, content, request.getSourceType(), request.getVideoUrl(), request.getQrCodeUrl(),
                request.getDuration());
        videoMapper.insert(video);
        BizLogger.info("video.create", "contentId={}", content.getId());
        return videoConverter.toDetail(content, video);
    }

    @RequirePermission(PermissionCodes.VIDEO_MANAGE)
    @Transactional
    public VideoDetailVO update(Long contentId, @Valid VideoUpdateRequest request) {
        Content content = requireVideoContent(contentId, false);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Video video = findVideo(contentId);
        boolean creatingVideo = video == null;
        if (creatingVideo) {
            video = new Video();
            video.setContentId(contentId);
        }
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        applyVideo(video, content, request.getSourceType(), request.getVideoUrl(), request.getQrCodeUrl(),
                request.getDuration());
        if (creatingVideo) {
            videoMapper.insert(video);
        } else {
            videoMapper.updateById(video);
        }
        BizLogger.info("video.update", "contentId={}", contentId);
        return videoConverter.toDetail(content, video);
    }

    private void applyVideo(Video video, Content content, VideoSourceType sourceType,
                            String videoUrl, String qrCodeUrl, Integer duration) {
        if (sourceType != null && sourceType.inAppPlayback()) {
            HttpUrls.requireMediaUrlIfPresent(videoUrl, "视频地址不合法");
        } else {
            HttpUrls.requireHttpIfPresent(videoUrl, "视频地址不合法");
        }
        HttpUrls.requireMediaUrlIfPresent(qrCodeUrl, "二维码地址不合法");
        video.setTitle(content.getTitle());
        video.setCoverUrl(content.getCoverUrl());
        video.setSourceType(sourceType);
        video.setVideoUrl(videoUrl == null ? "" : videoUrl.trim());
        video.setQrCodeUrl(TextValues.trimToNull(qrCodeUrl));
        video.setDuration(duration);
        video.setStatus(content.getStatus());
    }

    public VideoDetailVO publicDetail(Long contentId) {
        Content content = requireVideoContent(contentId, true);
        return videoConverter.toDetail(content, requireVideo(contentId));
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public VideoDetailVO adminDetail(Long contentId) {
        Content content = requireVideoContent(contentId, false);
        Video video = findVideo(contentId);
        if (video == null) {
            video = new Video();
            video.setContentId(contentId);
            video.setTitle(content.getTitle());
            video.setCoverUrl(content.getCoverUrl());
            video.setStatus(content.getStatus());
        }
        return videoConverter.toDetail(content, video);
    }

    private Content requireVideoContent(Long contentId, boolean publishedOnly) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.VIDEO) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        if (publishedOnly && content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private Video requireVideo(Long contentId) {
        Video video = findVideo(contentId);
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return video;
    }

    private Video findVideo(Long contentId) {
        return videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .select(Video::getId, Video::getContentId, Video::getTitle, Video::getCoverUrl,
                        Video::getSourceType, Video::getVideoUrl, Video::getQrCodeUrl, Video::getDuration,
                        Video::getStatus)
                .eq(Video::getContentId, contentId));
    }

    private Content newContent(VideoCreateRequest request) {
        Content content = new Content();
        content.setContentType(ContentType.VIDEO);
        content.setStatus(ContentStatus.DRAFT);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
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
