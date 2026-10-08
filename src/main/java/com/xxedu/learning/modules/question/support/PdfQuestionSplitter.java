package com.xxedu.learning.modules.question.support;

import com.xxedu.learning.modules.question.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按常见中文试卷题号规则切分 PDF 文本，并尝试分离题干 / 答案 / 解析。
 */
public final class PdfQuestionSplitter {

    private static final int MAX_QUESTIONS = 100;
    private static final int TITLE_MAX = 128;
    private static final Pattern QUESTION_START = Pattern.compile(
            "(?m)^\\s*(?:"
                    + "第\\s*(\\d{1,3})\\s*题"
                    + "|(\\d{1,3})\\s*[.、．)]"
                    + "|[（(]\\s*(\\d{1,3})\\s*[）)]"
                    + ")\\s*");
    private static final Pattern ANSWER_SPLIT = Pattern.compile(
            "(?m)^\\s*(?:【\\s*答案\\s*】|答案)\\s*[:：]?\\s*");
    private static final Pattern ANALYSIS_SPLIT = Pattern.compile(
            "(?m)^\\s*(?:【\\s*解析\\s*】|解析|解答)\\s*[:：]?\\s*");
    private static final Pattern CHOICE_OPTION = Pattern.compile(
            "(?m)^\\s*[A-DＡ-Ｄ][.、．)\\s]");
    private static final Pattern MULTI_HINT = Pattern.compile("多选|多项选择");
    private static final Pattern FILL_HINT = Pattern.compile("_{2,}|（\\s*）|\\(\\s*\\)|填空");
    private static final Pattern PROOF_HINT = Pattern.compile("证明|求证");

    private PdfQuestionSplitter() {
    }

    public static List<SplitQuestion> split(String rawText) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return List.of();
        }
        Matcher matcher = QUESTION_START.matcher(text);
        List<Integer> starts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            labels.add(resolveLabel(matcher));
        }
        if (starts.isEmpty()) {
            return List.of(toQuestion(1, "1", text, false));
        }
        List<SplitQuestion> questions = new ArrayList<>();
        int limit = Math.min(starts.size(), MAX_QUESTIONS);
        for (int i = 0; i < limit; i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
            String block = text.substring(start, end).trim();
            if (block.isBlank()) {
                continue;
            }
            questions.add(toQuestion(questions.size() + 1, labels.get(i), block, true));
        }
        markNumberGaps(questions);
        return questions;
    }

    private static SplitQuestion toQuestion(int index, String label, String block, boolean numbered) {
        String body = stripLeadingNumber(block);
        Sections sections = splitSections(body);
        QuestionType type = detectType(sections.questionText());
        List<String> warnings = new ArrayList<>();
        if (!numbered) {
            warnings.add("未识别到题号，已作为整份文本导入");
        }
        if (sections.questionText().length() < 8) {
            warnings.add("题干过短，请核对");
        }
        if (sections.answerText().isBlank()) {
            warnings.add("未识别到答案");
        }
        if (type == QuestionType.ANSWER && CHOICE_OPTION.matcher(sections.questionText()).find()) {
            warnings.add("疑似选择题，请确认题型");
        }
        String title = buildTitle(label, sections.questionText());
        return new SplitQuestion(
                index,
                label,
                title,
                type,
                sections.questionText(),
                sections.answerText(),
                sections.analysisText(),
                !warnings.isEmpty(),
                warnings.isEmpty() ? null : String.join("；", warnings));
    }

    private static void markNumberGaps(List<SplitQuestion> questions) {
        Integer prev = null;
        for (int i = 0; i < questions.size(); i++) {
            SplitQuestion question = questions.get(i);
            Integer current = parseNumber(question.label());
            if (prev != null && current != null && current != prev + 1) {
                String reason = question.suspiciousReason() == null
                        ? "题号不连续，请核对是否漏题"
                        : question.suspiciousReason() + "；题号不连续，请核对是否漏题";
                questions.set(i, question.withSuspicious(reason));
            }
            if (current != null) {
                prev = current;
            }
        }
    }

    private static Integer parseNumber(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(label.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String resolveLabel(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) {
                return matcher.group(i);
            }
        }
        return String.valueOf(matcher.groupCount());
    }

    private static String stripLeadingNumber(String block) {
        Matcher matcher = QUESTION_START.matcher(block);
        if (matcher.find() && matcher.start() == 0) {
            return block.substring(matcher.end()).trim();
        }
        return block.trim();
    }

    private static Sections splitSections(String body) {
        Matcher answerMatcher = ANSWER_SPLIT.matcher(body);
        if (!answerMatcher.find()) {
            return new Sections(body.trim(), "", "");
        }
        String questionText = body.substring(0, answerMatcher.start()).trim();
        String afterAnswer = body.substring(answerMatcher.end());
        Matcher analysisMatcher = ANALYSIS_SPLIT.matcher(afterAnswer);
        if (!analysisMatcher.find()) {
            return new Sections(questionText, afterAnswer.trim(), "");
        }
        String answerText = afterAnswer.substring(0, analysisMatcher.start()).trim();
        String analysisText = afterAnswer.substring(analysisMatcher.end()).trim();
        return new Sections(questionText, answerText, analysisText);
    }

    private static QuestionType detectType(String questionText) {
        String text = questionText == null ? "" : questionText;
        if (PROOF_HINT.matcher(text).find()) {
            return QuestionType.PROOF;
        }
        long options = CHOICE_OPTION.matcher(text).results().count();
        if (options >= 2) {
            return MULTI_HINT.matcher(text).find() ? QuestionType.MULTIPLE_CHOICE : QuestionType.SINGLE_CHOICE;
        }
        if (FILL_HINT.matcher(text).find()) {
            return QuestionType.FILL_BLANK;
        }
        return QuestionType.ANSWER;
    }

    private static String buildTitle(String label, String questionText) {
        String preview = questionText == null ? "" : questionText.replaceAll("\\s+", " ").trim();
        String base = "第" + label + "题";
        if (preview.isBlank()) {
            return base;
        }
        String combined = base + " " + preview;
        if (combined.length() <= TITLE_MAX) {
            return combined;
        }
        return combined.substring(0, TITLE_MAX);
    }

    private static String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .trim();
    }

    private record Sections(String questionText, String answerText, String analysisText) {
    }

    public record SplitQuestion(
            int index,
            String label,
            String title,
            QuestionType questionType,
            String questionText,
            String answerText,
            String analysisText,
            boolean suspicious,
            String suspiciousReason
    ) {
        private SplitQuestion withSuspicious(String reason) {
            return new SplitQuestion(index, label, title, questionType, questionText, answerText, analysisText,
                    true, reason);
        }
    }
}
