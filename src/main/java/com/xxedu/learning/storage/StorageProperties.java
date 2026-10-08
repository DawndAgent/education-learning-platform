package com.xxedu.learning.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * local：本地目录。cos：腾讯云对象存储。
     */
    private String type = "local";

    private final Local local = new Local();

    private final Cos cos = new Cos();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "./data/uploads";
        private String publicUrlPrefix = "/uploads";
    }

    @Getter
    @Setter
    public static class Cos {
        private String secretId = "";
        private String secretKey = "";
        private String region = "";
        private String bucket = "";
        /**
         * 对外访问前缀，如 https://bucket.cos.ap-guangzhou.myqcloud.com 或 CDN 域名。
         */
        private String publicBaseUrl = "";
    }
}
