package com.xxedu.learning.modules.file.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.file.enums.UploadScene;
import com.xxedu.learning.modules.file.service.FileService;
import com.xxedu.learning.modules.file.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传")
@RestController
@RequestMapping(ApiConstants.ADMIN_FILES)
@RequiredArgsConstructor
public class FileAdminController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "scene", required = false) String scene) {
        return ApiResponse.ok(fileService.uploadImage(file, UploadScene.from(scene)));
    }
}
