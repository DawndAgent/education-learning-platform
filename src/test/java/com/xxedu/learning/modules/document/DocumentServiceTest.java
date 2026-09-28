package com.xxedu.learning.modules.document;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.document.dto.DocumentCreateRequest;
import com.xxedu.learning.modules.document.dto.DocumentUpdateRequest;
import com.xxedu.learning.modules.document.service.DocumentService;
import com.xxedu.learning.modules.document.vo.DocumentDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class DocumentServiceTest extends IntegrationTestSupport {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ContentService contentService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createDefaultsDownloadAndPreviewToFileUrl() {
        DocumentCreateRequest request = create("讲义");
        request.setDownloadUrl(null);
        request.setPreviewUrl(" ");
        DocumentDetailVO created = documentService.create(request);

        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getFileUrl()).isEqualTo("https://example.com/files/a.pdf");
        assertThat(created.getDownloadUrl()).isEqualTo("https://example.com/files/a.pdf");
        assertThat(created.getPreviewUrl()).isEqualTo("https://example.com/files/a.pdf");
        assertThatThrownBy(() -> documentService.publicDetail(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资料不存在");
    }

    @Test
    void updateAndPublishValidation() {
        DocumentCreateRequest incompleteCreate = create("资料草稿");
        incompleteCreate.setFileUrl(null);
        incompleteCreate.setFileName(null);
        DocumentDetailVO created = documentService.create(incompleteCreate);
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资料文件不能为空");

        DocumentUpdateRequest ready = new DocumentUpdateRequest();
        ready.setCategoryId(12L);
        ready.setTitle("正式资料");
        ready.setSort(2);
        ready.setFileUrl("https://example.com/files/ready.pdf");
        ready.setFileName("ready.pdf");
        ready.setFileSize(1024L);
        ready.setFileType("application/pdf");
        ready.setDownloadUrl("https://example.com/dl/ready.pdf");
        ready.setPreviewUrl("https://example.com/preview/ready.pdf");
        DocumentDetailVO updated = documentService.update(created.getContentId(), ready);

        assertThat(updated.getDownloadUrl()).isEqualTo("https://example.com/dl/ready.pdf");
        assertThat(updated.getPreviewUrl()).isEqualTo("https://example.com/preview/ready.pdf");
        assertThat(contentService.publish(created.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(documentService.publicDetail(created.getContentId()).getFileName()).isEqualTo("ready.pdf");
    }

    private DocumentCreateRequest create(String title) {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setCoverUrl("https://example.com/cover.png");
        request.setSummary("摘要");
        request.setSort(1);
        request.setFileUrl("https://example.com/files/a.pdf");
        request.setFileName("a.pdf");
        request.setFileSize(2048L);
        request.setFileType("application/pdf");
        request.setDescription("描述");
        return request;
    }
}
