package com.xxedu.learning.modules.topic.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.topic.dto.TopicQueryRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.PublicTopicDetailVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "专题")
@RestController
@RequestMapping(ApiConstants.PUBLIC_TOPICS)
@RequiredArgsConstructor
public class TopicPublicController {

    private final TopicService topicService;

    @Operation(summary = "已发布专题列表")
    @GetMapping
    public ApiResponse<PageResult<PublicTopicListVO>> page(@Valid TopicQueryRequest request) {
        return ApiResponse.ok(topicService.publicPage(request));
    }

    @Operation(summary = "首页专题精选")
    @GetMapping("/featured")
    public ApiResponse<List<PublicTopicListVO>> featured(
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(topicService.publicFeatured(pageSize));
    }

    @Operation(summary = "已发布专题详情")
    @GetMapping("/{id}")
    public ApiResponse<PublicTopicDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(topicService.publicDetail(id));
    }
}
