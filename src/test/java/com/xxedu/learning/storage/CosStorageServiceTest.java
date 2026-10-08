package com.xxedu.learning.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.xxedu.learning.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosStorageServiceTest {

    @Mock
    private COSClient cosClient;

    private CosStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setType("cos");
        properties.getCos().setSecretId("sid");
        properties.getCos().setSecretKey("skey");
        properties.getCos().setRegion("ap-guangzhou");
        properties.getCos().setBucket("demo-1250000000");
        properties.getCos().setPublicBaseUrl("https://cdn.example.com/");
        storageService = new CosStorageService(properties, cosClient);
        storageService.init();
    }

    @Test
    void getUrlJoinsPublicBaseAndObjectKey() {
        assertThat(storageService.getUrl("videos/local/a.mp4"))
                .isEqualTo("https://cdn.example.com/videos/local/a.mp4");
        assertThat(storageService.getUrl("/images/covers/b.png"))
                .isEqualTo("https://cdn.example.com/images/covers/b.png");
    }

    @Test
    void rejectsIllegalObjectKey() {
        assertThatThrownBy(() -> CosStorageService.normalizeObjectKey("../x.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("非法的对象键");
        assertThatThrownBy(() -> CosStorageService.normalizeObjectKey(" "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("对象键不能为空");
    }

    @Test
    void uploadPutsObjectAndReturnsAbsoluteUrl() {
        when(cosClient.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        byte[] bytes = new byte[] {1, 2, 3};
        StorageUploadResult result = storageService.upload(
                new ByteArrayInputStream(bytes), "videos/local/demo.mp4", "video/mp4", bytes.length);

        assertThat(result.getObjectKey()).isEqualTo("videos/local/demo.mp4");
        assertThat(result.getUrl()).isEqualTo("https://cdn.example.com/videos/local/demo.mp4");
        assertThat(result.getContentType()).isEqualTo("video/mp4");
        assertThat(result.getSize()).isEqualTo(3);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cosClient).putObject(captor.capture());
        assertThat(captor.getValue().getBucketName()).isEqualTo("demo-1250000000");
        assertThat(captor.getValue().getKey()).isEqualTo("videos/local/demo.mp4");
        assertThat(captor.getValue().getMetadata().getContentLength()).isEqualTo(3);
        assertThat(captor.getValue().getMetadata().getContentType()).isEqualTo("video/mp4");
    }

    @Test
    void deleteDelegatesToClient() {
        storageService.delete("images/covers/a.png");
        verify(cosClient).deleteObject(eq("demo-1250000000"), eq("images/covers/a.png"));
    }

    @Test
    void initRequiresCosSettings() {
        StorageProperties empty = new StorageProperties();
        CosStorageService service = new CosStorageService(empty, cosClient);
        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage.cos.secret-id");
    }

    @Test
    void mapsArrearsCosErrorToReadableMessage() {
        CosServiceException ex = new CosServiceException("blocked");
        ex.setStatusCode(451);
        ex.setErrorCode("UnavailableForLegalReasons");
        ex.setErrorMessage("Due to your account is arrears, it is unavailable until you recharge.");
        assertThat(CosStorageService.cosUploadMessage(ex)).contains("欠费");
    }
}
