package com.xxedu.learning.modules.weekly;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.weekly.dto.WeeklyCreateRequest;
import com.xxedu.learning.modules.weekly.dto.WeeklyUpdateRequest;
import com.xxedu.learning.modules.weekly.service.WeeklyService;
import com.xxedu.learning.modules.weekly.vo.WeeklyDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class WeeklyServiceTest extends IntegrationTestSupport {

    @Autowired
    private WeeklyService weeklyService;

    @Autowired
    private ContentService contentService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createUpdateAndPublicDraftIsHidden() {
        WeeklyDetailVO created = weeklyService.create(create("第1周"));
        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getWeekLabel()).isEqualTo("2026-W01");
        assertThatThrownBy(() -> weeklyService.publicDetail(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("每周一题不存在");

        WeeklyUpdateRequest update = new WeeklyUpdateRequest();
        update.setCategoryId(12L);
        update.setTitle("第2周");
        update.setSort(2);
        update.setWeekLabel("2026-W02");
        update.setQuestionText("新题干");
        update.setAnswerText("新答案");
        WeeklyDetailVO updated = weeklyService.update(created.getContentId(), update);

        assertThat(updated.getTitle()).isEqualTo("第2周");
        assertThat(updated.getWeekLabel()).isEqualTo("2026-W02");
        assertThat(weeklyService.adminDetail(created.getContentId()).getAnswerText()).isEqualTo("新答案");
    }

    @Test
    void publishRequiresWeekLabelAndQa() {
        WeeklyCreateRequest missingLabel = create("缺周次");
        missingLabel.setWeekLabel(" ");
        WeeklyDetailVO noLabel = weeklyService.create(missingLabel);
        assertThatThrownBy(() -> contentService.publish(noLabel.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("周次标签不能为空");

        WeeklyCreateRequest missingAnswer = create("缺答案");
        missingAnswer.setWeekLabel("2026-W03");
        missingAnswer.setAnswerText(null);
        missingAnswer.setAnswerImageUrl(null);
        WeeklyDetailVO noAnswer = weeklyService.create(missingAnswer);
        assertThatThrownBy(() -> contentService.publish(noAnswer.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("答案不能为空");

        WeeklyUpdateRequest withAnswer = new WeeklyUpdateRequest();
        withAnswer.setCategoryId(11L);
        withAnswer.setTitle("缺答案");
        withAnswer.setSort(1);
        withAnswer.setWeekLabel("2026-W03");
        withAnswer.setQuestionText("题干");
        withAnswer.setAnswerText("答案");
        weeklyService.update(noAnswer.getContentId(), withAnswer);
        assertThat(contentService.publish(noAnswer.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(weeklyService.publicDetail(noAnswer.getContentId()).getWeekLabel()).isEqualTo("2026-W03");
    }

    private WeeklyCreateRequest create(String title) {
        WeeklyCreateRequest request = new WeeklyCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setCoverUrl("https://example.com/cover.png");
        request.setSummary("摘要");
        request.setSort(1);
        request.setWeekLabel("2026-W01");
        request.setQuestionText("题干");
        request.setAnswerText("答案");
        request.setAnalysisText("解析");
        return request;
    }
}
