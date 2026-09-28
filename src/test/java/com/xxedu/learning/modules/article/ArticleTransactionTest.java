package com.xxedu.learning.modules.article;

import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.dto.ArticleUpdateRequest;
import com.xxedu.learning.modules.article.mapper.ArticleMapper;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ArticleTransactionTest extends IntegrationTestSupport {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ArticleMapper articleMapper;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void contentInsertRollsBackWhenArticleInsertFails() {
        when(articleMapper.insert(any(com.xxedu.learning.modules.article.entity.Article.class)))
                .thenThrow(new RuntimeException("article insert failed"));
        int before = count("content");

        assertThatThrownBy(() -> articleService.create(request()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("article insert failed");

        assertThat(count("content")).isEqualTo(before);
        assertThat(count("article")).isEqualTo(0);
    }

    @Test
    void contentUpdateRollsBackWhenArticleInsertFails() {
        when(articleMapper.insert(any(com.xxedu.learning.modules.article.entity.Article.class)))
                .thenThrow(new RuntimeException("article insert failed"));
        ContentCreateRequest content = new ContentCreateRequest();
        content.setCategoryId(11L);
        content.setTitle("原标题");
        content.setContentType(ContentType.ARTICLE);
        content.setSort(1);
        Long id = contentService.create(content).getId();
        try {
            ArticleUpdateRequest update = new ArticleUpdateRequest();
            update.setCategoryId(11L);
            update.setTitle("不应保留");
            update.setSort(1);
            update.setBody("正文");
            assertThatThrownBy(() -> articleService.update(id, update))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("article insert failed");
            assertThat(contentService.adminDetail(id).getTitle()).isEqualTo("原标题");
        } finally {
            contentService.delete(id);
        }
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table + " where deleted = 0", Integer.class);
        return count == null ? 0 : count;
    }

    private ArticleCreateRequest request() {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle("回滚文章");
        request.setSort(1);
        request.setBody("正文");
        return request;
    }
}
