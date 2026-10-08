package com.xxedu.learning.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageConfigTest {

    @Test
    void localTypeCreatesLocalStorageService() {
        StorageProperties properties = new StorageProperties();
        properties.setType("local");
        StorageService service = new StorageConfig(properties).storageService();
        assertThat(service).isInstanceOf(LocalStorageService.class);
    }

    @Test
    void cosTypeCreatesCosStorageService() {
        StorageProperties properties = new StorageProperties();
        properties.setType("cos");
        properties.getCos().setSecretId("sid");
        properties.getCos().setSecretKey("skey");
        properties.getCos().setRegion("ap-guangzhou");
        properties.getCos().setBucket("bucket-125");
        properties.getCos().setPublicBaseUrl("https://cdn.example.com");
        StorageService service = new StorageConfig(properties).storageService();
        assertThat(service).isInstanceOf(CosStorageService.class);
    }

    @Test
    void s3AndOssAreRejectedExplicitly() {
        StorageProperties properties = new StorageProperties();
        properties.setType("s3");
        assertThatThrownBy(() -> new StorageConfig(properties).storageService())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未实现");
        properties.setType("oss");
        assertThatThrownBy(() -> new StorageConfig(properties).storageService())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未实现");
    }
}
