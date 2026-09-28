package com.xxedu.learning.modules.content;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.dto.ContentQueryRequest;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContentPublicOpsTest extends IntegrationTestSupport {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void keywordSearchMatchesTitleAndIgnoresDraft() throws Exception {
        ArticleDetailVO draft = articleService.create(article("KET 草稿"));
        ArticleDetailVO published = articleService.create(article("KET 高频词"));
        contentService.publish(published.getContentId());

        mockMvc.perform(get("/api/content").param("keyword", "KET").param("sort", "publishTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("KET 高频词"));

        assertThat(contentService.publicPage(keywordQuery("数学")).getTotal()).isZero();
        String draftStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM content WHERE id = ?", String.class, draft.getContentId());
        assertThat(draftStatus).isEqualTo("DRAFT");
    }

    @Test
    void emptyKeywordReturnsPublishedList() {
        articleService.create(article("空关键字草稿"));
        ArticleDetailVO published = articleService.create(article("空关键字已发布"));
        contentService.publish(published.getContentId());

        ContentQueryRequest query = new ContentQueryRequest();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setKeyword("  ");
        query.setSort("publishTime");
        assertThat(contentService.publicPage(query).getRecords())
                .extracting(ContentListVO::getTitle)
                .contains("空关键字已发布")
                .doesNotContain("空关键字草稿");
    }

    @Test
    void recordViewIncrementsPublishedOnly() throws Exception {
        ArticleDetailVO draft = articleService.create(article("浏览草稿"));
        ArticleDetailVO published = articleService.create(article("浏览已发布"));
        contentService.publish(published.getContentId());

        mockMvc.perform(post("/api/content/" + published.getContentId() + "/view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        ContentDetailVO after = contentService.publicDetail(published.getContentId());
        assertThat(after.getViewCount()).isEqualTo(1L);

        Integer draftViews = jdbcTemplate.queryForObject(
                "SELECT view_count FROM content WHERE id = ?", Integer.class, draft.getContentId());
        assertThat(draftViews).isZero();

        mockMvc.perform(post("/api/content/" + draft.getContentId() + "/view"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));

        TestAuth.loginOperator();
        contentService.publish(draft.getContentId());
        contentService.offline(draft.getContentId());
        mockMvc.perform(post("/api/content/" + draft.getContentId() + "/view"))
                .andExpect(status().isNotFound());

        assertThatThrownBy(() -> contentService.recordView(999999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private ArticleCreateRequest article(String title) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setBody("正文");
        request.setAuthor("老师");
        return request;
    }

    private ContentQueryRequest keywordQuery(String keyword) {
        ContentQueryRequest query = new ContentQueryRequest();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setKeyword(keyword);
        query.setSort("publishTime");
        return query;
    }
}
