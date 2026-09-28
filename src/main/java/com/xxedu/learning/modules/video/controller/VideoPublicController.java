package com.xxedu.learning.modules.video.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "视频")
@RestController
@RequestMapping(ApiConstants.PUBLIC_VIDEOS)
@RequiredArgsConstructor
public class VideoPublicController {

    private final VideoService videoService;

    @Operation(summary = "已发布视频详情")
    @GetMapping("/{contentId}")
    public ApiResponse<VideoDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(videoService.publicDetail(contentId));
    }
}
