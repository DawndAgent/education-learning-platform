package com.xxedu.learning.modules.question.convert;

import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.question.entity.Question;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionConverter {

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "categoryId", source = "content.categoryId")
    @Mapping(target = "coverUrl", source = "content.coverUrl")
    @Mapping(target = "summary", source = "content.summary")
    @Mapping(target = "status", source = "content.status")
    @Mapping(target = "sort", source = "content.sort")
    @Mapping(target = "publishTime", source = "content.publishTime")
    @Mapping(target = "questionType", source = "question.questionType")
    @Mapping(target = "questionText", source = "question.questionText")
    @Mapping(target = "questionImageUrl", source = "question.questionImageUrl")
    @Mapping(target = "answerText", source = "question.answerText")
    @Mapping(target = "answerImageUrl", source = "question.answerImageUrl")
    @Mapping(target = "analysisText", source = "question.analysisText")
    @Mapping(target = "analysisImageUrl", source = "question.analysisImageUrl")
    @Mapping(target = "difficulty", source = "question.difficulty")
    QuestionDetailVO toDetail(Content content, Question question);
}
