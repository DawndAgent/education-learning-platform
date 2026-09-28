package com.xxedu.learning.modules.question;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.question.dto.QuestionCreateRequest;
import com.xxedu.learning.modules.question.dto.QuestionUpdateRequest;
import com.xxedu.learning.modules.question.enums.QuestionDifficulty;
import com.xxedu.learning.modules.question.enums.QuestionType;
import com.xxedu.learning.modules.question.service.QuestionService;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class QuestionServiceTest extends IntegrationTestSupport {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ContentService contentService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createUpdateAndPublicDraftIsHidden() {
        QuestionDetailVO created = questionService.create(create("阅读单选"));
        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getQuestionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(created.getDifficulty()).isEqualTo(QuestionDifficulty.EASY);
        assertThatThrownBy(() -> questionService.publicDetail(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("题目不存在");

        QuestionUpdateRequest update = new QuestionUpdateRequest();
        update.setCategoryId(12L);
        update.setTitle("更新后的题目");
        update.setSort(2);
        update.setQuestionType(QuestionType.ANSWER);
        update.setQuestionText("新题干");
        update.setAnswerText("新答案");
        update.setDifficulty(QuestionDifficulty.HARD);
        QuestionDetailVO updated = questionService.update(created.getContentId(), update);

        assertThat(updated.getTitle()).isEqualTo("更新后的题目");
        assertThat(updated.getQuestionType()).isEqualTo(QuestionType.ANSWER);
        assertThat(updated.getDifficulty()).isEqualTo(QuestionDifficulty.HARD);
        assertThat(questionService.adminDetail(created.getContentId()).getAnswerText()).isEqualTo("新答案");
    }

    @Test
    void publishRequiresQuestionAndAnswer() {
        QuestionCreateRequest incomplete = create("缺答案");
        incomplete.setAnswerText(null);
        incomplete.setAnswerImageUrl(null);
        QuestionDetailVO created = questionService.create(incomplete);
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("答案不能为空");

        QuestionUpdateRequest withAnswer = new QuestionUpdateRequest();
        withAnswer.setCategoryId(11L);
        withAnswer.setTitle("缺答案");
        withAnswer.setSort(1);
        withAnswer.setQuestionType(QuestionType.SINGLE_CHOICE);
        withAnswer.setQuestionText("题干");
        withAnswer.setAnswerImageUrl("https://example.com/answer.png");
        questionService.update(created.getContentId(), withAnswer);

        assertThat(contentService.publish(created.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(questionService.publicDetail(created.getContentId()).getAnswerImageUrl())
                .isEqualTo("https://example.com/answer.png");
    }

    @Test
    void publishRejectsEmptyQuestionBody() {
        QuestionCreateRequest emptyQuestion = create("空题干");
        emptyQuestion.setQuestionText(" ");
        emptyQuestion.setQuestionImageUrl(null);
        QuestionDetailVO created = questionService.create(emptyQuestion);
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("题目内容不能为空");
    }

    private QuestionCreateRequest create(String title) {
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setCoverUrl("https://example.com/cover.png");
        request.setSummary("摘要");
        request.setSort(1);
        request.setQuestionType(QuestionType.SINGLE_CHOICE);
        request.setQuestionText("题干");
        request.setAnswerText("答案");
        request.setAnalysisText("解析");
        request.setDifficulty(QuestionDifficulty.EASY);
        return request;
    }
}
