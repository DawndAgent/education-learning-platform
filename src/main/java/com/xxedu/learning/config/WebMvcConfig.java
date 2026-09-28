package com.xxedu.learning.config;

import com.xxedu.learning.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        register(registry, "/api/**");
        register(registry, "/admin/api/**");
        register(registry, "/uploads/**");
    }

    private void register(CorsRegistry registry, String pattern) {
        if (corsProperties.getAllowedOrigins() == null || corsProperties.getAllowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping(pattern)
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders(SecurityConstants.TRACE_HEADER)
                .allowCredentials(true)
                .maxAge(3600);
    }
}
