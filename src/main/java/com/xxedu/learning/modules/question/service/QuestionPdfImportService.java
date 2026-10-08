package com.xxedu.learning.modules.question.service;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.modules.file.support.DocumentUploadValidator;
import com.xxedu.learning.modules.question.dto.QuestionBatchCreateRequest;
import com.xxedu.learning.modules.question.dto.QuestionCreateRequest;
import com.xxedu.learning.modules.question.support.PdfQuestionSplitter;
import com.xxedu.learning.modules.question.support.PdfTextExtractor;
import com.xxedu.learning.modules.question.vo.QuestionBatchCreateResultVO;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
import com.xxedu.learning.modules.question.vo.QuestionPdfParseResultVO;
import com.xxedu.learning.modules.question.vo.QuestionPdfParsedItemVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
public class QuestionPdfImportService {

    private final QuestionService questionService;

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    public QuestionPdfParseResultVO parse(MultipartFile file) {
        DocumentUploadValidator.ValidatedDocument validated = DocumentUploadValidator.validate(file);
        if (!"pdf".equals(validated.extension())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题目导入仅支持 PDF 文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败");
        }
        PdfTextExtractor.ExtractedPdf extracted = PdfTextExtractor.extract(bytes);
        List<PdfQuestionSplitter.SplitQuestion> split = PdfQuestionSplitter.split(extracted.text());
        if (split.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未能识别出题目，请检查 PDF 文本与题号格式");
        }

        QuestionPdfParseResultVO result = new QuestionPdfParseResultVO();
        result.setFileName(displayName(file.getOriginalFilename(), validated.fileName()));
        result.setPageCount(extracted.pageCount());
        result.setQuestionCount(split.size());
        List<QuestionPdfParsedItemVO> items = new ArrayList<>(split.size());
        for (PdfQuestionSplitter.SplitQuestion question : split) {
            QuestionPdfParsedItemVO item = new QuestionPdfParsedItemVO();
            item.setIndex(question.index());
            item.setLabel(question.label());
            item.setTitle(question.title());
            item.setQuestionType(question.questionType());
            item.setQuestionText(question.questionText());
            item.setAnswerText(question.answerText());
            item.setAnalysisText(question.analysisText());
            item.setSuspicious(question.suspicious());
            item.setSuspiciousReason(question.suspiciousReason());
            items.add(item);
        }
        result.setQuestions(items);
        BizLogger.info("question.pdf.parse", "fileName={} pages={} questions={}",
                result.getFileName(), result.getPageCount(), result.getQuestionCount());
        return result;
    }

    @RequirePermission(PermissionCodes.CONTENT_CREATE)
    @Transactional
    public QuestionBatchCreateResultVO batchCreate(@Valid QuestionBatchCreateRequest request) {
        List<Long> contentIds = new ArrayList<>(request.getItems().size());
        for (QuestionCreateRequest item : request.getItems()) {
            QuestionDetailVO created = questionService.create(item);
            contentIds.add(created.getContentId());
        }
        QuestionBatchCreateResultVO result = new QuestionBatchCreateResultVO();
        result.setCreatedCount(contentIds.size());
        result.setContentIds(contentIds);
        BizLogger.info("question.pdf.batchCreate", "count={}", contentIds.size());
        return result;
    }

    private static String displayName(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.toLowerCase(Locale.ROOT).endsWith(".pdf") ? name : fallback;
    }
}
