package com.xxedu.learning.modules.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockMiniProgramCodeClientTest {

    @Test
    void createsPngBytes() {
        byte[] png = new MockMiniProgramCodeClient()
                .createUnlimitedCode("12345", "pages/content-detail/content-detail", "develop");
        assertThat(png.length).isGreaterThan(100);
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }
}
