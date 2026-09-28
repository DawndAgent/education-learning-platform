package com.xxedu.learning.modules.content;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContentApiTest extends IntegrationTestSupport {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void publicCategoryTreeAndContentListAreAnonymous() throws Exception {
        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].name").value("剑桥英语"))
                .andExpect(jsonPath("$.data[0].children[0].code").value("CAMBRIDGE_KET"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CAMBRIDGE"));

        mockMvc.perform(get("/api/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void invalidPageSizeIsRejected() throws Exception {
        mockMvc.perform(get("/api/content").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void adminCategoryWriteChecksAuthenticationAndPermission() throws Exception {
        String body = """
                {"parentId":0,"name":"测试分类","code":"TEST_NODE","sort":9,"status":"ENABLED"}
                """;

        mockMvc.perform(post("/admin/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));

        mockMvc.perform(post("/admin/api/categories")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_VIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));

        mockMvc.perform(post("/admin/api/categories")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CATEGORY_MANAGE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.code").value("TEST_NODE"));

        mockMvc.perform(post("/admin/api/categories")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CATEGORY_MANAGE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":0,"name":"坏编码","code":"bad","sort":1,"status":"ENABLED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void articleCanBePublishedAndReadPublicly() throws Exception {
        String token = token(PermissionCodes.CONTENT_CREATE, PermissionCodes.CONTENT_PUBLISH, PermissionCodes.CONTENT_VIEW);
        String created = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":11,"title":"公开文章","sort":1,"body":"正文内容","author":"老师"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String contentId = created.replaceAll("(?s).*\"contentId\"\\s*:\\s*\"(\\d+)\".*", "$1");

        mockMvc.perform(get("/api/articles/" + contentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));

        mockMvc.perform(post("/admin/api/contents/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/articles/" + contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文内容"))
                .andExpect(jsonPath("$.data.title").value("公开文章"));

        mockMvc.perform(get("/api/content").param("keyword", "公开"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("公开文章"));

        mockMvc.perform(get("/admin/api/contents"))
                .andExpect(status().isUnauthorized());
    }

    private String token(String... permissions) {
        return jwtTokenService.issue(new LoginUser(8L, "admin", ClientType.ADMIN, Set.of(permissions)));
    }
}
