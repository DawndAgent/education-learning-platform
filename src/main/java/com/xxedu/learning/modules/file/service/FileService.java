package com.xxedu.learning.modules.file.service;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.file.enums.UploadScene;
import com.xxedu.learning.modules.file.support.DocumentUploadValidator;
import com.xxedu.learning.modules.file.support.ImageUploadValidator;
import com.xxedu.learning.modules.file.support.VideoUploadValidator;
import com.xxedu.learning.modules.file.vo.FileUploadVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import com.xxedu.learning.storage.StorageService;
import com.xxedu.learning.storage.StorageUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageService storageService;

    @RequirePermission(PermissionCodes.FILE_UPLOAD)
    public FileUploadVO uploadImage(MultipartFile file, UploadScene scene) {
        UploadScene resolved = scene == null ? UploadScene.COVER : scene;
        if (resolved == UploadScene.DOCUMENT) {
            DocumentUploadValidator.ValidatedDocument validated = DocumentUploadValidator.validate(file);
            return store(file, objectKey(resolved, validated.extension(), "files/"),
                    validated.contentType(), validated.size(), validated.fileName());
        }
        if (resolved == UploadScene.VIDEO_FILE) {
            VideoUploadValidator.ValidatedVideo validated = VideoUploadValidator.validate(file);
            return store(file, objectKey(resolved, validated.extension(), "videos/"),
                    validated.contentType(), validated.size(), validated.fileName());
        }
        ImageUploadValidator.ValidatedImage validated = ImageUploadValidator.validate(file);
        return store(file, objectKey(resolved, validated.extension(), "images/"),
                validated.contentType(), validated.size(), validated.fileName());
    }

    private FileUploadVO store(MultipartFile file, String objectKey, String contentType, long size, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            StorageUploadResult stored = storageService.upload(inputStream, objectKey, contentType, size);
            FileUploadVO vo = toVo(stored, fileName);
            BizLogger.info("file.upload", "objectKey={} size={} type={}",
                    vo.getObjectKey(), vo.getSize(), vo.getContentType());
            return vo;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败");
        }
    }

    private String objectKey(UploadScene scene, String extension, String prefix) {
        return prefix + scene.folder() + "/" + LocalDate.now().format(DAY)
                + "/" + UUID.randomUUID() + "." + extension;
    }

    private FileUploadVO toVo(StorageUploadResult stored, String fileName) {
        FileUploadVO vo = new FileUploadVO();
        vo.setUrl(stored.getUrl());
        vo.setObjectKey(stored.getObjectKey());
        vo.setFileName(fileName);
        vo.setContentType(stored.getContentType());
        vo.setSize(stored.getSize());
        return vo;
    }
}
