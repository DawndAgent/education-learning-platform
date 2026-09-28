package com.xxedu.learning.modules.document.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.document.convert.DocumentConverter;
import com.xxedu.learning.modules.document.dto.DocumentCreateRequest;
import com.xxedu.learning.modules.document.dto.DocumentUpdateRequest;
import com.xxedu.learning.modules.document.entity.Document;
import com.xxedu.learning.modules.document.mapper.DocumentMapper;
import com.xxedu.learning.modules.document.vo.DocumentDetailVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class DocumentService {

    private static final String NOT_FOUND = "资料不存在";

    private final ContentMapper contentMapper;
    private final DocumentMapper documentMapper;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final DocumentConverter documentConverter;

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public DocumentDetailVO create(@Valid DocumentCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = newContent(request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.insert(content);
        Document document = new Document();
        document.setContentId(content.getId());
        applyDocument(document, request.getFileUrl(), request.getFileName(), request.getFileSize(),
                request.getFileType(), request.getDownloadUrl(), request.getPreviewUrl(), request.getDescription());
        documentMapper.insert(document);
        BizLogger.info("document.create", "contentId={}", content.getId());
        return documentConverter.toDetail(content, document);
    }

    @RequirePermission(PermissionCodes.CONTENT_UPDATE)
    @Transactional
    public DocumentDetailVO update(Long contentId, @Valid DocumentUpdateRequest request) {
        Content content = requireDocumentContent(contentId, false);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Document document = findDocument(contentId);
        boolean creating = document == null;
        if (creating) {
            document = new Document();
            document.setContentId(contentId);
        }
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        applyDocument(document, request.getFileUrl(), request.getFileName(), request.getFileSize(),
                request.getFileType(), request.getDownloadUrl(), request.getPreviewUrl(), request.getDescription());
        if (creating) {
            documentMapper.insert(document);
        } else {
            documentMapper.updateById(document);
        }
        BizLogger.info("document.update", "contentId={}", contentId);
        return documentConverter.toDetail(content, document);
    }

    public DocumentDetailVO publicDetail(Long contentId) {
        Content content = requireDocumentContent(contentId, true);
        return documentConverter.toDetail(content, requireDocument(contentId));
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public DocumentDetailVO adminDetail(Long contentId) {
        Content content = requireDocumentContent(contentId, false);
        Document document = findDocument(contentId);
        if (document == null) {
            document = new Document();
            document.setContentId(contentId);
        }
        return documentConverter.toDetail(content, document);
    }

    private void applyDocument(Document document, String fileUrl, String fileName, Long fileSize, String fileType,
                               String downloadUrl, String previewUrl, String description) {
        String resolvedFileUrl = TextValues.trimToNull(fileUrl);
        document.setFileUrl(resolvedFileUrl);
        document.setFileName(TextValues.trimToNull(fileName));
        document.setFileSize(fileSize);
        document.setFileType(TextValues.trimToNull(fileType));
        String resolvedDownload = TextValues.trimToNull(downloadUrl);
        document.setDownloadUrl(resolvedDownload == null ? resolvedFileUrl : resolvedDownload);
        String resolvedPreview = TextValues.trimToNull(previewUrl);
        document.setPreviewUrl(resolvedPreview == null ? resolvedFileUrl : resolvedPreview);
        document.setDescription(TextValues.trimToNull(description));
    }

    private Content requireDocumentContent(Long contentId, boolean publishedOnly) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.DOCUMENT) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        if (publishedOnly && content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private Document requireDocument(Long contentId) {
        Document document = findDocument(contentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return document;
    }

    private Document findDocument(Long contentId) {
        return documentMapper.selectOne(Wrappers.<Document>lambdaQuery()
                .select(Document::getId, Document::getContentId, Document::getFileUrl, Document::getFileName,
                        Document::getFileSize, Document::getFileType, Document::getDownloadUrl,
                        Document::getPreviewUrl, Document::getDescription)
                .eq(Document::getContentId, contentId));
    }

    private Content newContent(Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        Content content = new Content();
        content.setContentType(ContentType.DOCUMENT);
        content.setStatus(ContentStatus.DRAFT);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        applyCatalog(content, categoryId, title, coverUrl, summary, sort);
        return content;
    }

    private void applyCatalog(Content content, Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        content.setCategoryId(categoryId);
        content.setTitle(title.trim());
        content.setCoverUrl(TextValues.trimToNull(coverUrl));
        content.setSummary(TextValues.trimToNull(summary));
        content.setSort(sort);
    }
}
