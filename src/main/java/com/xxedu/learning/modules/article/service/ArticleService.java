package com.xxedu.learning.modules.article.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.HtmlSanitizer;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.article.convert.ArticleConverter;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.dto.ArticleUpdateRequest;
import com.xxedu.learning.modules.article.entity.Article;
import com.xxedu.learning.modules.article.mapper.ArticleMapper;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ArticleService {

    private static final String NOT_FOUND = "文章不存在";

    private final ContentMapper contentMapper;
    private final ArticleMapper articleMapper;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final ArticleConverter articleConverter;

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public ArticleDetailVO create(@Valid ArticleCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = newContent(request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.insert(content);
        Article article = new Article();
        article.setContentId(content.getId());
        article.setBody(cleanBody(request.getBody()));
        article.setAuthor(TextValues.trimToNull(request.getAuthor()));
        article.setSource(TextValues.trimToNull(request.getSource()));
        articleMapper.insert(article);
        BizLogger.info("article.create", "contentId={}", content.getId());
        return articleConverter.toDetail(content, article);
    }

    @RequirePermission(PermissionCodes.CONTENT_UPDATE)
    @Transactional
    public ArticleDetailVO update(Long contentId, @Valid ArticleUpdateRequest request) {
        Content content = requireArticleContent(contentId, false);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Article article = findArticle(contentId);
        boolean creatingBody = article == null;
        if (creatingBody) {
            article = new Article();
            article.setContentId(contentId);
        }
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        article.setBody(cleanBody(request.getBody()));
        article.setAuthor(TextValues.trimToNull(request.getAuthor()));
        article.setSource(TextValues.trimToNull(request.getSource()));
        if (creatingBody) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }
        BizLogger.info("article.update", "contentId={}", contentId);
        return articleConverter.toDetail(content, article);
    }

    public ArticleDetailVO publicDetail(Long contentId) {
        Content content = requireArticleContent(contentId, true);
        return articleConverter.toDetail(content, requireArticle(contentId));
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public ArticleDetailVO adminDetail(Long contentId) {
        Content content = requireArticleContent(contentId, false);
        Article article = findArticle(contentId);
        if (article == null) {
            article = new Article();
            article.setContentId(contentId);
            article.setBody("");
        }
        return articleConverter.toDetail(content, article);
    }

    private Content requireArticleContent(Long contentId, boolean publishedOnly) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.ARTICLE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        if (publishedOnly && content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private Article requireArticle(Long contentId) {
        Article article = findArticle(contentId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return article;
    }

    private String cleanBody(String body) {
        String cleaned = HtmlSanitizer.clean(body);
        if (!HtmlSanitizer.hasText(cleaned)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章正文不能为空");
        }
        return cleaned;
    }

    private Article findArticle(Long contentId) {
        return articleMapper.selectOne(Wrappers.<Article>lambdaQuery()
                .select(Article::getId, Article::getContentId, Article::getBody, Article::getAuthor, Article::getSource)
                .eq(Article::getContentId, contentId));
    }

    private Content newContent(Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        Content content = new Content();
        content.setContentType(ContentType.ARTICLE);
        content.setStatus(ContentStatus.DRAFT);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        applyCatalog(content, categoryId, title, coverUrl, summary, sort);
        return content;
    }

    private void applyCatalog(Content content, Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        content.setCategoryId(categoryId);
        content.setTitle(title.trim());
        content.setCoverUrl(TextValues.trimToNull(coverUrl));
        content.setSummary(TextValues.trimToNull(summary));
        content.setSort(sort);
    }
}
