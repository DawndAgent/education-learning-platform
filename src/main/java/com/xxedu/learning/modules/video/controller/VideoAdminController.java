package com.xxedu.learning.modules.video.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.dto.VideoUpdateRequest;
import com.xxedu.learning.modules.video.service.VideoMiniprogramQrService;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.MiniprogramQrVO;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "视频管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_VIDEOS)
@RequiredArgsConstructor
public class VideoAdminController {

    private final VideoService videoService;
    private final VideoMiniprogramQrService videoMiniprogramQrService;

    @Operation(summary = "新增视频")
    @PostMapping
    public ApiResponse<VideoDetailVO> create(@Valid @RequestBody VideoCreateRequest request) {
        return ApiResponse.ok(videoService.create(request));
    }

    @Operation(summary = "修改视频")
    @PutMapping("/{contentId}")
    public ApiResponse<VideoDetailVO> update(@PathVariable Long contentId,
                                             @Valid @RequestBody VideoUpdateRequest request) {
        return ApiResponse.ok(videoService.update(contentId, request));
    }

    @Operation(summary = "视频详情")
    @GetMapping("/{contentId}")
    public ApiResponse<VideoDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(videoService.adminDetail(contentId));
    }

    @Operation(summary = "生成或获取打开小程序视频详情的小程序码")
    @PostMapping("/{contentId}/miniprogram-qrcode")
    public ApiResponse<MiniprogramQrVO> miniprogramQrcode(@PathVariable Long contentId,
                                                          @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.ok(videoMiniprogramQrService.ensureMiniprogramQr(contentId, force));
    }
}
