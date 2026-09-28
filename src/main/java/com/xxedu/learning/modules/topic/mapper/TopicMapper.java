package com.xxedu.learning.modules.topic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxedu.learning.modules.topic.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {

    IPage<Topic> selectTopicPage(Page<Topic> page, @Param("query") TopicPageQuery query);

    List<TopicContentCountRow> countContentsByTopicIds(@Param("topicIds") List<Long> topicIds);

    long countPublishedContents(@Param("topicId") Long topicId);
}
