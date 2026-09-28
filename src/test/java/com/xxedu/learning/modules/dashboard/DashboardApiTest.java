package com.xxedu.learning.modules.dashboard;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class DashboardApiTest extends IntegrationTestSupport {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void overviewRequiresDashboardView() throws Exception {
        mockMvc.perform(get("/admin/api/dashboard/overview")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_VIEW)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void overviewAggregatesContentWithoutDeleted() throws Exception {
        String token = token(
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.CONTENT_CREATE,
                PermissionCodes.CONTENT_PUBLISH,
                PermissionCodes.CONTENT_OFFLINE,
                PermissionCodes.CONTENT_DELETE,
                PermissionCodes.CONTENT_VIEW,
                PermissionCodes.VIDEO_MANAGE);

        String articlePublished = createArticle(token, 11, "看板文章A");
        String articleDraft = createArticle(token, 11, "看板文章草稿");
        String articleOffline = createArticle(token, 21, "看板文章下线");
        String videoPublished = createVideo(token, 11, "看板视频A");
        String deletedArticle = createArticle(token, 21, "看板已删");

        publish(token, articlePublished);
        publish(token, articleOffline);
        publish(token, videoPublished);
        offline(token, articleOffline);
        deleteContent(token, deletedArticle);

        String articleLatest = createArticle(token, 11, "看板最新发布");
        publish(token, articleLatest);
        jdbcTemplate.update(
                "UPDATE content SET publish_time = TIMESTAMPADD(SECOND, 1, publish_time) WHERE id = ?",
                Long.parseLong(articleLatest));

        mockMvc.perform(get("/admin/api/dashboard/overview")
                        .header("Authorization", "Bearer " + token(PermissionCodes.DASHBOARD_VIEW)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.contentTotal").value(5))
                .andExpect(jsonPath("$.data.publishedCount").value(3))
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.offlineCount").value(1))
                .andExpect(jsonPath("$.data.articleCount").value(4))
                .andExpect(jsonPath("$.data.videoCount").value(1))
                .andExpect(jsonPath("$.data.weekNewCount").value(5))
                .andExpect(jsonPath("$.data.monthNewCount").value(5))
                .andExpect(jsonPath("$.data.categoryStats[?(@.categoryId=='1')].count", contains(4)))
                .andExpect(jsonPath("$.data.categoryStats[?(@.categoryId=='2')].count", contains(1)))
                .andExpect(jsonPath("$.data.recentPublished", hasSize(3)))
                .andExpect(jsonPath("$.data.recentPublished[0].title").value("看板最新发布"))
                .andExpect(jsonPath("$.data.recentPublished[0].contentType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.recentPublished[0].categoryName").value("KET/PET备考资料"));
    }

    @Test
    void adminRoleHasDashboardViewSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM sys_role_permission rp
                                 INNER JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
                        WHERE rp.role_id = 1
                          AND rp.deleted = 0
                          AND p.permission_code = 'DASHBOARD_VIEW'
                        """,
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    private String createArticle(String token, long categoryId, String title) throws Exception {
        String body = """
                {"categoryId":%d,"title":"%s","sort":1,"body":"正文","author":"老师"}
                """.formatted(categoryId, title);
        String created = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return created.replaceAll("(?s).*\"contentId\"\\s*:\\s*\"(\\d+)\".*", "$1");
    }

    private String createVideo(String token, long categoryId, String title) throws Exception {
        String body = """
                {"categoryId":%d,"title":"%s","sort":1,"summary":"简介","coverUrl":"https://example.com/c.png",
                "sourceType":"WECHAT_CHANNEL","videoUrl":"https://channels.weixin.qq.com/example",
                "qrCodeUrl":"https://example.com/qr.png","duration":60}
                """.formatted(categoryId, title);
        String created = mockMvc.perform(post("/admin/api/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return created.replaceAll("(?s).*\"contentId\"\\s*:\\s*\"(\\d+)\".*", "$1");
    }

    private void publish(String token, String contentId) throws Exception {
        mockMvc.perform(post("/admin/api/contents/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void offline(String token, String contentId) throws Exception {
        mockMvc.perform(post("/admin/api/contents/" + contentId + "/offline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void deleteContent(String token, String contentId) throws Exception {
        mockMvc.perform(delete("/admin/api/contents/" + contentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String token(String... permissions) {
        LoginUser user = new LoginUser(9L, "tester", ClientType.ADMIN, Set.of(permissions));
        return jwtTokenService.issue(user);
    }
}
