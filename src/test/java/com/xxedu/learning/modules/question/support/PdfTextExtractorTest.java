package com.xxedu.learning.modules.question.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    @Test
    void extractsTextAndSplitsQuestions() throws IOException {
        byte[] pdf = samplePdf("""
                1. Choice question text long enough.
                A. One
                B. Two
                C. Three
                D. Four
                Answer: A
                2. Fill blank ____ here.
                Answer: 2
                """);

        PdfTextExtractor.ExtractedPdf extracted = PdfTextExtractor.extract(pdf);
        assertThat(extracted.pageCount()).isEqualTo(1);
        assertThat(extracted.text()).contains("Choice question");

        List<PdfQuestionSplitter.SplitQuestion> questions = PdfQuestionSplitter.split(extracted.text());
        assertThat(questions).hasSize(2);
    }

    private static byte[] samplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
                    stream.showText(line.replace('\n', ' '));
                    stream.newLineAtOffset(0, -16);
                }
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
