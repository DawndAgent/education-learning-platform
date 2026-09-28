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
}
