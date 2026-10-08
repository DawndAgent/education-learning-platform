package com.xxedu.learning.modules.auth;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.security.JwtProperties;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestSupport {

    private static final String PASSWORD_HASH =
            "$2a$10$lB/26dYn6x1QywlQDX3B2e8T/M0OBCQ8OihSNCcer5nBblqiHn8Vy";

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginSuccessReturnsTokenAndPermissions() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.user.id").value("1"))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.nickname").value("管理员"))
                .andExpect(jsonPath("$.data.user.permissions.length()").value(19))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void unknownUsernameIsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing","password":"Admin@123456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    void disabledAccountIsUnauthorized() throws Exception {
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = 2");
        jdbcTemplate.update("""
                INSERT INTO sys_user (
                    id, username, password_hash, nickname, status, created_at, updated_at, deleted
                ) VALUES (
                    2, 'disabled', ?, '停用', 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0
                )
                """, PASSWORD_HASH);
        try {
            mockMvc.perform(post("/admin/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"disabled","password":"Admin@123456"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("账号已禁用"));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = 2");
        }
    }

    @Test
    void currentUserRequiresValidToken() throws Exception {
        String body = mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.permissions.length()").value(19));

        mockMvc.perform(post("/admin/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        String token = Jwts.builder()
                .subject("admin")
                .claim("uid", 1L)
                .claim("clientType", ClientType.ADMIN.name())
                .claim("permissions", Set.of(PermissionCodes.CONTENT_VIEW))
                .issuedAt(new Date(past.getTime() - 60_000))
                .expiration(past)
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void deleteWithoutContentDeleteIsForbidden() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN, Set.of(PermissionCodes.CONTENT_VIEW)));

        mockMvc.perform(delete("/admin/api/contents/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void categoryManageWithoutPermissionIsForbidden() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN, Set.of(PermissionCodes.CONTENT_VIEW)));

        mockMvc.perform(delete("/admin/api/categories/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void loginPreflightFromAdminOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/admin/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
