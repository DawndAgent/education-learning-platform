package com.xxedu.learning.security;

import com.xxedu.learning.common.constant.ApiConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<AccessTokenParser> accessTokenParser,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiConstants.HEALTH).permitAll()
                        .requestMatchers(ApiConstants.PUBLIC + "/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                ApiConstants.PUBLIC_CATEGORIES,
                                ApiConstants.PUBLIC_CATEGORIES + "/**",
                                ApiConstants.PUBLIC_CONTENT,
                                ApiConstants.PUBLIC_CONTENT + "/**",
                                ApiConstants.PUBLIC_ARTICLES,
                                ApiConstants.PUBLIC_ARTICLES + "/**",
                                ApiConstants.PUBLIC_VIDEOS,
                                ApiConstants.PUBLIC_VIDEOS + "/**",
                                ApiConstants.PUBLIC_QUESTIONS,
                                ApiConstants.PUBLIC_QUESTIONS + "/**",
                                ApiConstants.PUBLIC_WEEKLIES,
                                ApiConstants.PUBLIC_WEEKLIES + "/**",
                                ApiConstants.PUBLIC_DOCUMENTS,
                                ApiConstants.PUBLIC_DOCUMENTS + "/**",
                                ApiConstants.PUBLIC_TOPICS,
                                ApiConstants.PUBLIC_TOPICS + "/**",
                                ApiConstants.PUBLIC_HOME).permitAll()
                        .requestMatchers(HttpMethod.POST, ApiConstants.PUBLIC_CONTENT + "/*/view").permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, ApiConstants.ADMIN_AUTH + "/login").permitAll()
                        .requestMatchers(HttpMethod.GET, ApiConstants.UPLOADS + "/**").permitAll()
                        .requestMatchers(ApiConstants.ADMIN + "/**").authenticated()
                        .requestMatchers(ApiConstants.ADMIN_API + "/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(
                        new BearerTokenAuthenticationFilter(accessTokenParser),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
