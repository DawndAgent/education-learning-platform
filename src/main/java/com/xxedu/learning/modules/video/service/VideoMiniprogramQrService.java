package com.xxedu.learning.modules.video.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.video.entity.Video;
import com.xxedu.learning.modules.video.mapper.VideoMapper;
import com.xxedu.learning.modules.video.vo.MiniprogramQrVO;
import com.xxedu.learning.modules.wechat.MiniProgramCodeClient;
import com.xxedu.learning.modules.wechat.WechatMiniappProperties;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import com.xxedu.learning.storage.StorageService;
import com.xxedu.learning.storage.StorageUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class VideoMiniprogramQrService {

    static final String DETAIL_PAGE = "pages/content-detail/content-detail";

    private final ContentService contentService;
    private final VideoMapper videoMapper;
    private final MiniProgramCodeClient miniProgramCodeClient;
    private final WechatMiniappProperties wechatMiniappProperties;
    private final StorageService storageService;

    @RequirePermission(PermissionCodes.FILE_UPLOAD)
    @Transactional
    public MiniprogramQrVO ensureMiniprogramQr(Long contentId, boolean force) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.VIDEO) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只能为视频内容生成小程序码");
        }
        Video video = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .eq(Video::getContentId, contentId));
        if (video == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
        if (!force && video.getMiniprogramQrUrl() != null && !video.getMiniprogramQrUrl().isBlank()) {
            return toVo(content, video.getMiniprogramQrUrl());
        }
        if (!wechatMiniappProperties.isMockEnabled() && !wechatMiniappProperties.credentialsConfigured()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "未配置微信小程序 AppID/AppSecret，无法生成小程序码");
        }

        String scene = String.valueOf(contentId);
        if (scene.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容 ID 过长，无法生成小程序码");
        }
        byte[] image = miniProgramCodeClient.createUnlimitedCode(
                scene, DETAIL_PAGE, wechatMiniappProperties.getEnvVersion());
        boolean jpeg = image.length >= 3
                && (image[0] & 0xFF) == 0xFF
                && (image[1] & 0xFF) == 0xD8;
        String ext = jpeg ? ".jpg" : ".png";
        String contentType = jpeg ? "image/jpeg" : "image/png";
        String objectKey = "images/qrcodes/miniprogram/v" + contentId + ext;
        StorageUploadResult stored = storageService.upload(
                new ByteArrayInputStream(image), objectKey, contentType, image.length);
        video.setMiniprogramQrUrl(stored.getUrl());
        videoMapper.updateById(video);
        BizLogger.info("video.miniprogram_qr", "contentId={} url={} mocked={}",
                contentId, stored.getUrl(), wechatMiniappProperties.useMock());
        return toVo(content, stored.getUrl());
    }

    private MiniprogramQrVO toVo(Content content, String url) {
        MiniprogramQrVO vo = new MiniprogramQrVO();
        vo.setContentId(content.getId());
        vo.setTitle(content.getTitle());
        vo.setUrl(url);
        vo.setMocked(wechatMiniappProperties.useMock());
        return vo;
    }
}
