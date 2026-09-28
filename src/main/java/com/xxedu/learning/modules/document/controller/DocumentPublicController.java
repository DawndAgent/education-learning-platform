package com.xxedu.learning.modules.document.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.document.service.DocumentService;
import com.xxedu.learning.modules.document.vo.DocumentDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "资料")
@RestController
@RequestMapping(ApiConstants.PUBLIC_DOCUMENTS)
@RequiredArgsConstructor
public class DocumentPublicController {

    private final DocumentService documentService;

    @Operation(summary = "已发布资料详情")
    @GetMapping("/{contentId}")
    public ApiResponse<DocumentDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(documentService.publicDetail(contentId));
    }
}
