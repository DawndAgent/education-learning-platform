package com.xxedu.learning.modules.question.support;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    public static ExtractedPdf extract(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "PDF 没有可识别的页面");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "未能从 PDF 提取文字，请确认是可选中文本的电子版 PDF（扫描件暂不支持）");
            }
            return new ExtractedPdf(document.getNumberOfPages(), text);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析 PDF 文件");
        }
    }

    public record ExtractedPdf(int pageCount, String text) {
    }
}
