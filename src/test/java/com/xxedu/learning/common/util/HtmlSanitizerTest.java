package com.xxedu.learning.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void keepsRelativeUploadImageSrcAndStripsScripts() {
        String cleaned = HtmlSanitizer.clean(
                "<p>ok</p><img src=\"/uploads/images/a.png\" alt=\"x\">"
                        + "<script>alert(1)</script><img src=\"javascript:alert(1)\">");
        assertThat(cleaned).contains("ok");
        assertThat(cleaned).contains("src=\"/uploads/images/a.png\"");
        assertThat(cleaned.toLowerCase()).doesNotContain("<script");
        assertThat(cleaned.toLowerCase()).doesNotContain("javascript:");
    }

    @Test
    void keepsVideoMiniprogramQrMarkup() {
        String cleaned = HtmlSanitizer.clean(
                "<p class=\"video-miniprogram-qr\" data-video-id=\"10001\">"
                        + "<img src=\"/uploads/images/qrcodes/miniprogram/v10001.png\" "
                        + "alt=\"扫码观看视频：听力课\" style=\"max-width:220px;height:auto;\" />"
                        + "<br/><span>扫码观看：听力课</span></p>");
        assertThat(cleaned).contains("data-video-id=\"10001\"");
        assertThat(cleaned).contains("video-miniprogram-qr");
        assertThat(cleaned).contains("/uploads/images/qrcodes/miniprogram/v10001.png");
    }
}
