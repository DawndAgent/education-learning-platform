package com.xxedu.learning.modules.question.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.question.convert.QuestionConverter;
import com.xxedu.learning.modules.question.dto.QuestionCreateRequest;
import com.xxedu.learning.modules.question.dto.QuestionUpdateRequest;
import com.xxedu.learning.modules.question.entity.Question;
import com.xxedu.learning.modules.question.enums.QuestionDifficulty;
import com.xxedu.learning.modules.question.enums.QuestionType;
import com.xxedu.learning.modules.question.mapper.QuestionMapper;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
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
public class QuestionService {

    private static final String NOT_FOUND = "题目不存在";

    private final ContentMapper contentMapper;
    private final QuestionMapper questionMapper;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final QuestionConverter questionConverter;

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public QuestionDetailVO create(@Valid QuestionCreateRequest request) {
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Content content = newContent(request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.insert(content);
        Question question = new Question();
        question.setContentId(content.getId());
        applyQuestion(question, request.getQuestionType(), request.getQuestionText(), request.getQuestionImageUrl(),
                request.getAnswerText(), request.getAnswerImageUrl(), request.getAnalysisText(),
                request.getAnalysisImageUrl(), request.getDifficulty());
        questionMapper.insert(question);
        BizLogger.info("question.create", "contentId={}", content.getId());
        return questionConverter.toDetail(content, question);
    }

    @RequirePermission(PermissionCodes.CONTENT_UPDATE)
    @Transactional
    public QuestionDetailVO update(Long contentId, @Valid QuestionUpdateRequest request) {
        Content content = requireQuestionContent(contentId, false);
        categoryService.assertContentCategory(request.getCategoryId(), false);
        Question question = findQuestion(contentId);
        boolean creating = question == null;
        if (creating) {
            question = new Question();
            question.setContentId(contentId);
        }
        applyCatalog(content, request.getCategoryId(), request.getTitle(), request.getCoverUrl(),
                request.getSummary(), request.getSort());
        contentMapper.updateById(content);
        applyQuestion(question, request.getQuestionType(), request.getQuestionText(), request.getQuestionImageUrl(),
                request.getAnswerText(), request.getAnswerImageUrl(), request.getAnalysisText(),
                request.getAnalysisImageUrl(), request.getDifficulty());
        if (creating) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }
        BizLogger.info("question.update", "contentId={}", contentId);
        return questionConverter.toDetail(content, question);
    }

    public QuestionDetailVO publicDetail(Long contentId) {
        Content content = requireQuestionContent(contentId, true);
        return questionConverter.toDetail(content, requireQuestion(contentId));
    }

    @RequirePermission(PermissionCodes.CONTENT_VIEW)
    public QuestionDetailVO adminDetail(Long contentId) {
        Content content = requireQuestionContent(contentId, false);
        Question question = findQuestion(contentId);
        if (question == null) {
            question = new Question();
            question.setContentId(contentId);
        }
        return questionConverter.toDetail(content, question);
    }

    private void applyQuestion(Question question, QuestionType questionType, String questionText,
                               String questionImageUrl, String answerText, String answerImageUrl,
                               String analysisText, String analysisImageUrl, QuestionDifficulty difficulty) {
        question.setQuestionType(questionType);
        question.setQuestionText(TextValues.trimToNull(questionText));
        question.setQuestionImageUrl(TextValues.trimToNull(questionImageUrl));
        question.setAnswerText(TextValues.trimToNull(answerText));
        question.setAnswerImageUrl(TextValues.trimToNull(answerImageUrl));
        question.setAnalysisText(TextValues.trimToNull(analysisText));
        question.setAnalysisImageUrl(TextValues.trimToNull(analysisImageUrl));
        question.setDifficulty(difficulty);
    }

    private Content requireQuestionContent(Long contentId, boolean publishedOnly) {
        Content content = contentService.requireContent(contentId);
        if (content.getContentType() != ContentType.QUESTION) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        if (publishedOnly && content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return content;
    }

    private Question requireQuestion(Long contentId) {
        Question question = findQuestion(contentId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return question;
    }

    private Question findQuestion(Long contentId) {
        return questionMapper.selectOne(Wrappers.<Question>lambdaQuery()
                .select(Question::getId, Question::getContentId, Question::getQuestionType,
                        Question::getQuestionText, Question::getQuestionImageUrl,
                        Question::getAnswerText, Question::getAnswerImageUrl,
                        Question::getAnalysisText, Question::getAnalysisImageUrl, Question::getDifficulty)
                .eq(Question::getContentId, contentId));
    }

    private Content newContent(Long categoryId, String title, String coverUrl, String summary, Integer sort) {
        Content content = new Content();
        content.setContentType(ContentType.QUESTION);
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
