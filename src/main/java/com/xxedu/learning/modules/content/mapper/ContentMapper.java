package com.xxedu.learning.modules.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.modules.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    IPage<Content> selectContentPage(Page<Content> page, @Param("query") ContentPageQuery query);

    List<ContentCountRow> countGroupByStatus();

    List<ContentCountRow> countGroupByContentType();

    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<ContentCategoryCountRow> countGroupByRootCategory();

    List<ContentRecentRow> selectRecentPublished(@Param("limit") int limit);

    int incrementViewCount(@Param("id") Long id);

    List<ContentDueRow> selectDueDrafts(@Param("now") LocalDateTime now, @Param("limit") int limit);

    long countScheduledDrafts();

    int publishFromDraft(@Param("id") Long id, @Param("publishTime") LocalDateTime publishTime);

    int publishFromOffline(@Param("id") Long id, @Param("publishTime") LocalDateTime publishTime);

    int clearScheduledPublishTime(@Param("id") Long id, @Param("now") LocalDateTime now);
}
