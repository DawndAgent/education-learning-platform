package com.xxedu.learning.config;

import com.xxedu.learning.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private static final List<String> CORS_PATHS = List.of("/api/**", "/admin/api/**", "/uploads/**");
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = buildCorsConfiguration();
        if (config != null) {
            for (String path : CORS_PATHS) {
                source.registerCorsConfiguration(path, config);
            }
        }
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsConfiguration config = buildCorsConfiguration();
        if (config == null) {
            return;
        }
        for (String path : CORS_PATHS) {
            registry.addMapping(path)
                    .allowedOrigins(config.getAllowedOrigins().toArray(String[]::new))
                    .allowedMethods(ALLOWED_METHODS.toArray(String[]::new))
                    .allowedHeaders("*")
                    .exposedHeaders(SecurityConstants.TRACE_HEADER)
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }

    private CorsConfiguration buildCorsConfiguration() {
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return null;
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.addExposedHeader(SecurityConstants.TRACE_HEADER);
        return config;
    }
}
