package com.xxedu.learning.modules.topic.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicContentSortRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemBatchRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemSortRequest;
import com.xxedu.learning.modules.topic.dto.TopicQueryRequest;
import com.xxedu.learning.modules.topic.dto.TopicUpdateRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.TopicContentItemVO;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.modules.topic.vo.TopicItemBatchResultVO;
import com.xxedu.learning.modules.topic.vo.TopicListVO;
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

import java.util.List;

@Tag(name = "专题管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_TOPICS)
@RequiredArgsConstructor
public class TopicAdminController {

    private final TopicService topicService;

    @Operation(summary = "专题列表")
    @GetMapping
    public ApiResponse<PageResult<TopicListVO>> page(@Valid TopicQueryRequest request) {
        return ApiResponse.ok(topicService.adminPage(request));
    }

    @Operation(summary = "专题详情")
    @GetMapping("/{id}")
    public ApiResponse<TopicDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(topicService.adminDetail(id));
    }

    @Operation(summary = "创建专题")
    @PostMapping
    public ApiResponse<TopicDetailVO> create(@Valid @RequestBody TopicCreateRequest request) {
        return ApiResponse.ok(topicService.create(request));
    }

    @Operation(summary = "编辑专题")
    @PutMapping("/{id}")
    public ApiResponse<TopicDetailVO> update(@PathVariable Long id, @Valid @RequestBody TopicUpdateRequest request) {
        return ApiResponse.ok(topicService.update(id, request));
    }

    @Operation(summary = "删除专题")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "发布专题")
    @PostMapping("/{id}/publish")
    public ApiResponse<TopicDetailVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(topicService.publish(id));
    }

    @Operation(summary = "下线专题")
    @PostMapping("/{id}/offline")
    public ApiResponse<TopicDetailVO> offline(@PathVariable Long id) {
        return ApiResponse.ok(topicService.offline(id));
    }

    @Operation(summary = "专题内容列表")
    @GetMapping("/{topicId}/items")
    public ApiResponse<List<TopicContentItemVO>> listItems(@PathVariable Long topicId) {
        return ApiResponse.ok(topicService.listContents(topicId));
    }

    @Operation(summary = "添加一条专题内容")
    @PostMapping("/{topicId}/items")
    public ApiResponse<TopicContentItemVO> addItem(
            @PathVariable Long topicId, @Valid @RequestBody TopicItemAddRequest request) {
        return ApiResponse.ok(topicService.addItem(topicId, request));
    }

    @Operation(summary = "批量添加专题内容")
    @PostMapping("/{topicId}/items/batch")
    public ApiResponse<TopicItemBatchResultVO> addItems(
            @PathVariable Long topicId, @Valid @RequestBody TopicItemBatchRequest request) {
        return ApiResponse.ok(topicService.addItems(topicId, request));
    }

    @Operation(summary = "按条目删除专题内容")
    @DeleteMapping("/{topicId}/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long topicId, @PathVariable Long itemId) {
        topicService.removeItem(topicId, itemId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "按条目调整专题内容排序")
    @PutMapping("/{topicId}/items/sort")
    public ApiResponse<List<TopicContentItemVO>> sortItems(
            @PathVariable Long topicId, @Valid @RequestBody TopicItemSortRequest request) {
        return ApiResponse.ok(topicService.sortItems(topicId, request));
    }

    @Operation(summary = "专题内容列表")
    @GetMapping("/{topicId}/contents")
    public ApiResponse<List<TopicContentItemVO>> listContents(@PathVariable Long topicId) {
        return ApiResponse.ok(topicService.listContents(topicId));
    }

    @Operation(summary = "添加专题内容")
    @PostMapping("/{topicId}/contents")
    public ApiResponse<List<TopicContentItemVO>> addContents(
            @PathVariable Long topicId, @Valid @RequestBody TopicContentAddRequest request) {
        return ApiResponse.ok(topicService.addContents(topicId, request));
    }

    @Operation(summary = "移除专题内容")
    @DeleteMapping("/{topicId}/contents/{contentId}")
    public ApiResponse<Void> removeContent(@PathVariable Long topicId, @PathVariable Long contentId) {
        topicService.removeContent(topicId, contentId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "专题内容排序")
    @PutMapping("/{topicId}/contents/sort")
    public ApiResponse<List<TopicContentItemVO>> sortContents(
            @PathVariable Long topicId, @Valid @RequestBody TopicContentSortRequest request) {
        return ApiResponse.ok(topicService.sortContents(topicId, request));
    }
}
