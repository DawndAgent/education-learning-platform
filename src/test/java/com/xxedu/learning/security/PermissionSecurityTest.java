package com.xxedu.learning.security;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.support.FrameworkProbeController;
import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionSecurityTest extends IntegrationTestSupport {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void adminWriteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/probe/write"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void authenticatedUserWithoutPermissionIsForbidden() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(7L, "admin", ClientType.ADMIN, Set.of("system:framework:read")));

        mockMvc.perform(post("/api/v1/admin/probe/write")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void authenticatedUserWithPermissionCanWrite() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(
                7L, "admin", ClientType.ADMIN, Set.of(FrameworkProbeController.WRITE_PERMISSION)));

        mockMvc.perform(post("/api/v1/admin/probe/write")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value("accepted"));
    }

    @Test
    void invalidTokenDoesNotBlockPublicApi() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
