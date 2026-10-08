package com.xxedu.learning.modules.question.support;

import com.xxedu.learning.modules.question.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfQuestionSplitterTest {

    @Test
    void splitsNumberedQuestionsWithAnswerAndAnalysis() {
        String text = """
                1. 下列哪项正确？
                A. 甲
                B. 乙
                C. 丙
                D. 丁
                答案：A
                解析：选甲。
                2. 证明：三角形内角和为 180 度。
                答案：略
                3. 填空：1+1=____。
                答案：2
                """;

        List<PdfQuestionSplitter.SplitQuestion> questions = PdfQuestionSplitter.split(text);

        assertThat(questions).hasSize(3);
        assertThat(questions.get(0).questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(questions.get(0).answerText()).isEqualTo("A");
        assertThat(questions.get(0).analysisText()).contains("选甲");
        assertThat(questions.get(1).questionType()).isEqualTo(QuestionType.PROOF);
        assertThat(questions.get(2).questionType()).isEqualTo(QuestionType.FILL_BLANK);
        assertThat(questions.get(2).answerText()).isEqualTo("2");
    }

    @Test
    void marksSuspiciousWhenAnswerMissingAndNumberGap() {
        String text = """
                1. 第一题题干足够长一些。
                3. 第三题题干也足够长一些。
                """;

        List<PdfQuestionSplitter.SplitQuestion> questions = PdfQuestionSplitter.split(text);

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).suspicious()).isTrue();
        assertThat(questions.get(0).suspiciousReason()).contains("未识别到答案");
        assertThat(questions.get(1).suspicious()).isTrue();
        assertThat(questions.get(1).suspiciousReason()).contains("题号不连续");
    }

    @Test
    void fallsBackToSingleBlockWhenNoNumberFound() {
        String text = "这是一段没有题号的练习说明文字，长度足够。";
        List<PdfQuestionSplitter.SplitQuestion> questions = PdfQuestionSplitter.split(text);
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).suspicious()).isTrue();
        assertThat(questions.get(0).questionText()).contains("没有题号");
    }
}
