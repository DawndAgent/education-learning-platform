package com.xxedu.learning.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig implements WebMvcConfigurer {

    private final StorageProperties properties;

    public StorageConfig(StorageProperties properties) {
        this.properties = properties;
    }

    @Bean
    public StorageService storageService() {
        String type = properties.getType() == null ? "local" : properties.getType().trim().toLowerCase();
        if ("local".equals(type)) {
            return new LocalStorageService(properties);
        }
        if ("cos".equals(type) || "s3".equals(type) || "oss".equals(type)) {
            return new ObjectStorageService();
        }
        throw new IllegalStateException("不支持的 storage.type: " + properties.getType());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(StringUtils.trimWhitespace(properties.getType()))) {
            return;
        }
        String prefix = properties.getLocal().getPublicUrlPrefix();
        if (!StringUtils.hasText(prefix)) {
            return;
        }
        String pattern = prefix.endsWith("/") ? prefix + "**" : prefix + "/**";
        Path root = Path.of(properties.getLocal().getBasePath()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(pattern)
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());
    }
}
