package com.xxedu.learning.modules.document.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.document.dto.DocumentCreateRequest;
import com.xxedu.learning.modules.document.dto.DocumentUpdateRequest;
import com.xxedu.learning.modules.document.service.DocumentService;
import com.xxedu.learning.modules.document.vo.DocumentDetailVO;
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

@Tag(name = "资料管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_DOCUMENTS)
@RequiredArgsConstructor
public class DocumentAdminController {

    private final DocumentService documentService;

    @Operation(summary = "新增资料")
    @PostMapping
    public ApiResponse<DocumentDetailVO> create(@Valid @RequestBody DocumentCreateRequest request) {
        return ApiResponse.ok(documentService.create(request));
    }

    @Operation(summary = "修改资料")
    @PutMapping("/{contentId}")
    public ApiResponse<DocumentDetailVO> update(@PathVariable Long contentId,
                                                @Valid @RequestBody DocumentUpdateRequest request) {
        return ApiResponse.ok(documentService.update(contentId, request));
    }

    @Operation(summary = "资料详情")
    @GetMapping("/{contentId}")
    public ApiResponse<DocumentDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(documentService.adminDetail(contentId));
    }
}
