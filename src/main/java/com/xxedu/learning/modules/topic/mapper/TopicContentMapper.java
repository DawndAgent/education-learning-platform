package com.xxedu.learning.modules.topic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxedu.learning.modules.topic.entity.TopicContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopicContentMapper extends BaseMapper<TopicContent> {

    List<TopicContentRow> selectAdminContents(@Param("topicId") Long topicId, @Param("limit") int limit);

    List<TopicContentRow> selectPublishedContents(@Param("topicId") Long topicId);

    Integer selectMaxSort(@Param("topicId") Long topicId);
}
