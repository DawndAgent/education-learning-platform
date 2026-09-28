package com.xxedu.learning.modules.article;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.category.dto.CategoryUpdateRequest;
import com.xxedu.learning.modules.category.enums.CategoryStatus;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.dto.ArticleUpdateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ArticleServiceTest extends IntegrationTestSupport {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createAndUpdateArticle() {
        ArticleDetailVO created = articleService.create(create());

        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getBody()).isEqualTo("正文");
        assertThat(created.getCoverUrl()).isEqualTo("https://example.com/cover.png");
        assertThat(articleService.adminDetail(created.getContentId()).getTitle()).isEqualTo("KET阅读");
        assertThatThrownBy(() -> articleService.publicDetail(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章不存在");

        ArticleUpdateRequest update = new ArticleUpdateRequest();
        update.setCategoryId(12L);
        update.setTitle("更新后的标题");
        update.setSort(3);
        update.setBody("新正文");
        update.setAuthor("新作者");
        ArticleDetailVO updated = articleService.update(created.getContentId(), update);

        assertThat(updated.getTitle()).isEqualTo("更新后的标题");
        assertThat(updated.getCategoryId()).isEqualTo(12L);
        assertThat(updated.getBody()).isEqualTo("新正文");
        assertThat(updated.getStatus()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void missingCategoryAndBlankTitleAreRejected() {
        ArticleCreateRequest request = create();
        request.setCategoryId(9999L);
        assertThatThrownBy(() -> articleService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        ArticleCreateRequest blank = create();
        blank.setTitle(" ");
        assertThatThrownBy(() -> articleService.create(blank))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void editorFillsBodyForContentCreatedWithoutArticle() {
        ContentCreateRequest content = new ContentCreateRequest();
        content.setContentType(ContentType.ARTICLE);
        content.setCategoryId(11L);
        content.setTitle("仅主表文章");
        content.setSort(1);
        var created = contentService.create(content);

        assertThat(articleService.adminDetail(created.getId()).getBody()).isEmpty();

        ArticleUpdateRequest update = new ArticleUpdateRequest();
        update.setCategoryId(11L);
        update.setTitle("仅主表文章");
        update.setSort(1);
        update.setBody("<p>第一段正文</p>");
        update.setAuthor("老师");
        ArticleDetailVO saved = articleService.update(created.getId(), update);

        assertThat(saved.getBody()).isEqualTo("<p>第一段正文</p>");
        assertThat(saved.getAuthor()).isEqualTo("老师");
        assertThatThrownBy(() -> articleService.publicDetail(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章不存在");

        contentService.publish(created.getId());
        assertThat(articleService.publicDetail(created.getId()).getBody()).isEqualTo("<p>第一段正文</p>");
    }

    @Test
    void stripsDangerousHtmlAndRejectsEmptyBodyOnPublish() {
        ArticleCreateRequest script = create();
        script.setBody("<p>安全正文</p><script>alert(1)</script><script src=\"https://evil.example/a.js\"></script>");
        ArticleDetailVO saved = articleService.create(script);
        assertThat(saved.getBody()).contains("安全正文");
        assertThat(saved.getBody().toLowerCase()).doesNotContain("<script");

        ArticleUpdateRequest image = updateBody(saved);
        image.setBody("<p>仍可展示</p><img src=\"https://example.com/a.png\" onerror=\"alert(1)\" "
                + "onclick=\"alert(1)\" onload=\"alert(1)\">"
                + "<iframe src=\"https://evil.example\"></iframe><object></object><embed src=\"https://evil.example\">");
        ArticleDetailVO updated = articleService.update(saved.getContentId(), image);
        assertThat(updated.getBody()).contains("仍可展示");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("onerror");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("onclick");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("onload");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("<iframe");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("<object");
        assertThat(updated.getBody().toLowerCase()).doesNotContain("<embed");
        assertThat(contentService.publish(saved.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);

        ArticleCreateRequest onlyScript = create();
        onlyScript.setTitle("只有脚本");
        onlyScript.setBody("<script>alert(1)</script>");
        assertThatThrownBy(() -> articleService.create(onlyScript))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章正文不能为空");

        ContentCreateRequest shell = new ContentCreateRequest();
        shell.setContentType(ContentType.ARTICLE);
        shell.setCategoryId(11L);
        shell.setTitle("没有正文");
        shell.setSort(1);
        var created = contentService.create(shell);
        assertThatThrownBy(() -> contentService.publish(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章正文不能为空");
    }

    @Test
    void disabledCategoryCannotSaveArticle() {
        CategoryUpdateRequest category = new CategoryUpdateRequest();
        category.setParentId(1L);
        category.setName("往期文章合集");
        category.setCode("CAMBRIDGE_ARCHIVE");
        category.setSort(4);
        category.setStatus(CategoryStatus.DISABLED);
        categoryService.update(14L, category);

        ArticleCreateRequest request = create();
        request.setCategoryId(14L);
        assertThatThrownBy(() -> articleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类已停用");
    }

    @Test
    void articleUpdateRequiresContentUpdate() {
        ArticleDetailVO created = articleService.create(create());
        TestAuth.login(PermissionCodes.CONTENT_VIEW);

        assertThatThrownBy(() -> articleService.update(created.getContentId(), updateBody(created)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private ArticleCreateRequest create() {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle("KET阅读");
        request.setCoverUrl(" https://example.com/cover.png ");
        request.setSummary("摘要");
        request.setSort(1);
        request.setBody("正文");
        request.setAuthor("作者");
        request.setSource("来源");
        return request;
    }

    private ArticleUpdateRequest updateBody(ArticleDetailVO current) {
        ArticleUpdateRequest request = new ArticleUpdateRequest();
        request.setCategoryId(current.getCategoryId());
        request.setTitle(current.getTitle());
        request.setSort(current.getSort());
        request.setBody("另一段正文");
        return request;
    }
}
