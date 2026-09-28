package com.xxedu.learning.modules.content.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.content.dto.ContentBatchRequest;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.dto.ContentQueryRequest;
import com.xxedu.learning.modules.content.dto.ContentUpdateRequest;
import com.xxedu.learning.modules.content.dto.SchedulePublishRequest;
import com.xxedu.learning.modules.content.service.ContentBatchPublishService;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentBatchResultVO;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "内容管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_CONTENTS)
@RequiredArgsConstructor
public class ContentAdminController {

    private final ContentService contentService;
    private final ContentBatchPublishService contentBatchPublishService;

    @Operation(summary = "内容列表")
    @GetMapping
    public ApiResponse<PageResult<ContentListVO>> page(@Valid ContentQueryRequest request) {
        return ApiResponse.ok(contentService.adminPage(request));
    }

    @Operation(summary = "内容详情")
    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.adminDetail(id));
    }

    @Operation(summary = "新增内容（已停用：请使用各类型专用接口）")
    @PostMapping
    public ApiResponse<ContentDetailVO> create(@Valid @RequestBody ContentCreateRequest request) {
        // 禁止仅创建 Content 主表，避免无子表的脏数据。
        // ContentService.create 仍保留给历史兼容与单测，但管理端 HTTP 入口关闭。
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请使用对应类型接口创建内容");
    }

    @Operation(summary = "编辑内容")
    @PutMapping("/{id}")
    public ApiResponse<ContentDetailVO> update(@PathVariable Long id, @Valid @RequestBody ContentUpdateRequest request) {
        return ApiResponse.ok(contentService.update(id, request));
    }

    @Operation(summary = "发布内容")
    @PostMapping("/{id}/publish")
    public ApiResponse<ContentDetailVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(contentService.publish(id));
    }

    @Operation(summary = "下架内容")
    @PostMapping("/{id}/offline")
    public ApiResponse<ContentDetailVO> offline(@PathVariable Long id) {
        return ApiResponse.ok(contentService.offline(id));
    }

    @Operation(summary = "定时发布")
    @PostMapping("/{id}/schedule-publish")
    public ApiResponse<ContentDetailVO> schedulePublish(@PathVariable Long id,
                                                        @Valid @RequestBody SchedulePublishRequest request) {
        return ApiResponse.ok(contentService.schedulePublish(id, request));
    }

    @Operation(summary = "取消定时发布")
    @PostMapping("/{id}/cancel-scheduled-publish")
    public ApiResponse<ContentDetailVO> cancelScheduledPublish(@PathVariable Long id) {
        return ApiResponse.ok(contentService.cancelScheduledPublish(id));
    }

    @Operation(summary = "删除内容")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contentService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "复制内容")
    @PostMapping("/{id}/duplicate")
    public ApiResponse<ContentDetailVO> duplicate(@PathVariable Long id) {
        return ApiResponse.ok(contentService.duplicate(id));
    }

    @Operation(summary = "批量立即发布")
    @PostMapping("/batch-publish")
    public ApiResponse<ContentBatchResultVO> batchPublish(@Valid @RequestBody ContentBatchRequest request) {
        return ApiResponse.ok(contentBatchPublishService.batchPublish(request.getIds()));
    }

    @Operation(summary = "批量下线")
    @PostMapping("/batch-offline")
    public ApiResponse<ContentBatchResultVO> batchOffline(@Valid @RequestBody ContentBatchRequest request) {
        return ApiResponse.ok(contentService.batchOffline(request.getIds()));
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    public ApiResponse<ContentBatchResultVO> batchDelete(@Valid @RequestBody ContentBatchRequest request) {
        return ApiResponse.ok(contentService.batchDelete(request.getIds()));
    }
}
