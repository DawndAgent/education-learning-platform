package com.xxedu.learning.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * local：本地目录。cos / s3：预留给对象存储实现。
     */
    private String type = "local";

    private final Local local = new Local();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "./data/uploads";
        private String publicUrlPrefix = "/uploads";
    }
}
