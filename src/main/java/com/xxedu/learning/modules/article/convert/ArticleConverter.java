package com.xxedu.learning.modules.article.convert;

import com.xxedu.learning.modules.article.entity.Article;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.entity.Content;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleConverter {

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "categoryId", source = "content.categoryId")
    @Mapping(target = "coverUrl", source = "content.coverUrl")
    @Mapping(target = "summary", source = "content.summary")
    @Mapping(target = "status", source = "content.status")
    @Mapping(target = "sort", source = "content.sort")
    @Mapping(target = "publishTime", source = "content.publishTime")
    @Mapping(target = "body", source = "article.body")
    @Mapping(target = "author", source = "article.author")
    @Mapping(target = "source", source = "article.source")
    ArticleDetailVO toDetail(Content content, Article article);
}
