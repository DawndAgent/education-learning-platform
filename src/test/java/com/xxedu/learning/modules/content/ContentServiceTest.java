package com.xxedu.learning.modules.content;

import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.category.dto.CategoryUpdateRequest;
import com.xxedu.learning.modules.category.enums.CategoryStatus;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.dto.ContentQueryRequest;
import com.xxedu.learning.modules.content.dto.ContentUpdateRequest;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ContentServiceTest extends IntegrationTestSupport {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void draftIsHiddenUntilPublishedAndPublishTimeStaysStable() {
        ArticleDetailVO article = articleService.create(article(11L, "KET听力专项", 2));
        ContentQueryRequest draftFilter = query(null, null);
        draftFilter.setStatus(ContentStatus.DRAFT);

        assertThat(contentService.publicPage(draftFilter).getRecords()).isEmpty();
        assertThatThrownBy(() -> contentService.publicDetail(article.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");

        ContentDetailVO published = contentService.publish(article.getContentId());
        LocalDateTime publishTime = published.getPublishTime();
        ContentDetailVO again = contentService.publish(article.getContentId());

        assertThat(published.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(again.getPublishTime()).isEqualTo(publishTime);
        assertThat(contentService.publicPage(draftFilter).getRecords())
                .extracting(ContentListVO::getId)
                .containsExactly(article.getContentId());
    }

    @Test
    void draftCannotGoOfflineAndPublishedCan() {
        ArticleDetailVO article = articleService.create(article(11L, "下架草稿", 1));

        assertThatThrownBy(() -> contentService.offline(article.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("草稿不能下架");

        contentService.publish(article.getContentId());
        ContentDetailVO offline = contentService.offline(article.getContentId());

        assertThat(offline.getStatus()).isEqualTo(ContentStatus.OFFLINE);
        assertThat(contentService.publicPage(query(null, null)).getRecords()).isEmpty();
        assertThat(contentService.offline(article.getContentId()).getStatus()).isEqualTo(ContentStatus.OFFLINE);
    }

    @Test
    void categoryFilterIncludesDescendantsAndKeywordIsLiteral() {
        articleService.create(article(11L, "KET听力专项", 1));
        ArticleDetailVO published = articleService.create(article(11L, "A_B练习", 2));
        contentService.publish(published.getContentId());
        articleService.create(article(21L, "数学思维题", 1));

        PageResult<ContentListVO> byParent = contentService.adminPage(query(1L, null));
        assertThat(byParent.getRecords()).extracting(ContentListVO::getTitle)
                .contains("KET听力专项", "A_B练习")
                .doesNotContain("数学思维题");

        ContentQueryRequest keyword = query(null, "听力");
        assertThat(contentService.adminPage(keyword).getRecords()).extracting(ContentListVO::getTitle)
                .containsExactly("KET听力专项");

        ContentQueryRequest underscore = query(null, "_");
        assertThat(contentService.adminPage(underscore).getRecords()).extracting(ContentListVO::getTitle)
                .containsExactly("A_B练习");
    }

    @Test
    void adminPageIsPaginatedAndDeleteRemovesContent() {
        ArticleDetailVO first = articleService.create(article(11L, "第一篇", 1));
        articleService.create(article(11L, "第二篇", 2));
        ContentQueryRequest request = query(11L, null);
        request.setPageNum(1);
        request.setPageSize(1);

        PageResult<ContentListVO> page = contentService.adminPage(request);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getContentType()).isEqualTo(ContentType.ARTICLE);

        contentService.delete(first.getContentId());
        assertThatThrownBy(() -> contentService.adminDetail(first.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
        assertThatThrownBy(() -> articleService.publicDetail(first.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void createArticleAndVideoThenFilterByTypeAndStatus() {
        ContentDetailVO article = contentService.create(content(ContentType.ARTICLE, 11L, "管理文章", 2));
        ContentDetailVO video = contentService.create(content(ContentType.VIDEO, 12L, "管理视频", 1));

        assertThat(article.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(article.getCategoryName()).isEqualTo("KET/PET备考资料");
        assertThat(article.getViewCount()).isZero();
        assertThat(article.getCreatedAt()).isNotNull();
        assertThat(video.getContentType()).isEqualTo(ContentType.VIDEO);
        assertThat(video.getCategoryName()).isEqualTo("剑桥原版阅读");

        ContentQueryRequest articles = query(null, null);
        articles.setContentType(ContentType.ARTICLE);
        assertThat(contentService.adminPage(articles).getRecords()).extracting(ContentListVO::getTitle)
                .contains("管理文章")
                .doesNotContain("管理视频");

        ContentQueryRequest drafts = query(null, null);
        drafts.setStatus(ContentStatus.DRAFT);
        assertThat(contentService.adminPage(drafts).getRecords()).extracting(ContentListVO::getTitle)
                .contains("管理文章", "管理视频");

        ContentQueryRequest bySort = query(12L, null);
        assertThat(contentService.adminPage(bySort).getRecords()).extracting(ContentListVO::getTitle)
                .containsExactly("管理视频");
    }

    @Test
    void rejectsInvalidCreateAndEditsExistingContent() {
        assertThatThrownBy(() -> contentService.create(content(ContentType.ARTICLE, 11L, " ", 1)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> contentService.create(content(ContentType.ARTICLE, 9999L, "不存在分类", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类不存在");
        assertThatThrownBy(() -> contentService.create(content(ContentType.ARTICLE, 1L, "一级分类", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容只能选择二级分类");
        assertThatThrownBy(() -> contentService.create(content(ContentType.TOPIC, 12L, "专题", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的内容类型");

        disableCategory(11L, 1L, "KET/PET备考资料", "CAMBRIDGE_KET", 1);
        assertThatThrownBy(() -> contentService.create(content(ContentType.ARTICLE, 11L, "停用分类", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类已停用");

        ContentDetailVO created = contentService.create(content(ContentType.ARTICLE, 12L, "原标题", 1));
        ContentDetailVO edited = contentService.update(created.getId(), update(created, "新标题", 13L, 4));
        assertThat(edited.getTitle()).isEqualTo("新标题");
        assertThat(edited.getCategoryId()).isEqualTo(13L);
        assertThat(edited.getContentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(edited.getStatus()).isEqualTo(ContentStatus.DRAFT);

        ContentUpdateRequest typeChange = update(created, "新标题", 13L, 4);
        typeChange.setContentType(ContentType.VIDEO);
        assertThatThrownBy(() -> contentService.update(created.getId(), typeChange))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容类型创建后不能修改");
        assertThatThrownBy(() -> contentService.update(9999L, update(created, "新标题", 13L, 4)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void publishOfflineAndDeleteFollowStatus() {
        ContentDetailVO blocked = contentService.create(content(ContentType.ARTICLE, 14L, "停用后发布", 1));
        disableCategory(14L, 1L, "往期文章合集", "CAMBRIDGE_ARCHIVE", 4);
        assertThatThrownBy(() -> contentService.publish(blocked.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类已停用，不能发布");
        assertThatThrownBy(() -> contentService.publish(9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
        assertThatThrownBy(() -> contentService.offline(9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");

        ContentDetailVO draft = contentService.create(content(ContentType.VIDEO, 13L, "可删除草稿", 1));
        contentService.delete(draft.getId());
        assertThatThrownBy(() -> contentService.adminDetail(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");

        ArticleDetailVO published = articleService.create(article(13L, "已发布文章", 2));
        assertThat(contentService.publish(published.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThatThrownBy(() -> contentService.delete(published.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先下架内容后再删除");
        assertThat(contentService.offline(published.getContentId()).getStatus()).isEqualTo(ContentStatus.OFFLINE);
        contentService.delete(published.getContentId());
        assertThatThrownBy(() -> contentService.adminDetail(published.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void contentPermissionsAreRequired() {
        ContentDetailVO created = contentService.create(content(ContentType.ARTICLE, 11L, "权限内容", 1));

        TestAuth.login(PermissionCodes.CONTENT_CREATE);
        assertForbidden(() -> contentService.adminPage(query(null, null)));

        TestAuth.login(PermissionCodes.CONTENT_VIEW);
        assertForbidden(() -> contentService.create(content(ContentType.ARTICLE, 11L, "无权新增", 1)));
        assertForbidden(() -> contentService.update(created.getId(), update(created, "无权编辑", 11L, 1)));
        assertForbidden(() -> contentService.delete(created.getId()));
        assertForbidden(() -> contentService.publish(created.getId()));
        assertForbidden(() -> contentService.offline(created.getId()));
    }

    private ArticleCreateRequest article(Long categoryId, String title, int sort) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(categoryId);
        request.setTitle(title);
        request.setCoverUrl(" https://example.com/cover.png ");
        request.setSummary("摘要");
        request.setSort(sort);
        request.setBody("正文");
        request.setAuthor("作者");
        request.setSource("来源");
        return request;
    }

    private ContentCreateRequest content(ContentType type, Long categoryId, String title, int sort) {
        ContentCreateRequest request = new ContentCreateRequest();
        request.setContentType(type);
        request.setCategoryId(categoryId);
        request.setTitle(title);
        request.setCoverUrl(" https://example.com/cover.png ");
        request.setSummary("摘要");
        request.setSort(sort);
        return request;
    }

    private ContentUpdateRequest update(ContentDetailVO current, String title, Long categoryId, int sort) {
        ContentUpdateRequest request = new ContentUpdateRequest();
        request.setContentType(current.getContentType());
        request.setCategoryId(categoryId);
        request.setTitle(title);
        request.setCoverUrl(current.getCoverUrl());
        request.setSummary(current.getSummary());
        request.setSort(sort);
        return request;
    }

    private void disableCategory(long id, long parentId, String name, String code, int sort) {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setParentId(parentId);
        request.setName(name);
        request.setCode(code);
        request.setSort(sort);
        request.setStatus(CategoryStatus.DISABLED);
        categoryService.update(id, request);
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private ContentQueryRequest query(Long categoryId, String keyword) {
        ContentQueryRequest request = new ContentQueryRequest();
        request.setCategoryId(categoryId);
        request.setKeyword(keyword);
        return request;
    }
}
