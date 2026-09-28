package com.xxedu.learning.modules.weekly.convert;

import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.weekly.entity.WeeklyQuestion;
import com.xxedu.learning.modules.weekly.vo.WeeklyDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklyConverter {

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "categoryId", source = "content.categoryId")
    @Mapping(target = "coverUrl", source = "content.coverUrl")
    @Mapping(target = "summary", source = "content.summary")
    @Mapping(target = "status", source = "content.status")
    @Mapping(target = "sort", source = "content.sort")
    @Mapping(target = "publishTime", source = "content.publishTime")
    @Mapping(target = "weekLabel", source = "weekly.weekLabel")
    @Mapping(target = "questionText", source = "weekly.questionText")
    @Mapping(target = "questionImageUrl", source = "weekly.questionImageUrl")
    @Mapping(target = "answerText", source = "weekly.answerText")
    @Mapping(target = "answerImageUrl", source = "weekly.answerImageUrl")
    @Mapping(target = "analysisText", source = "weekly.analysisText")
    @Mapping(target = "analysisImageUrl", source = "weekly.analysisImageUrl")
    WeeklyDetailVO toDetail(Content content, WeeklyQuestion weekly);
}
