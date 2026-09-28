package com.xxedu.learning.modules.video.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.dto.VideoUpdateRequest;
import com.xxedu.learning.modules.video.service.VideoService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "视频管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_VIDEOS)
@RequiredArgsConstructor
public class VideoAdminController {

    private final VideoService videoService;

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
}
