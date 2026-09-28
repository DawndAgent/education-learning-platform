package com.xxedu.learning.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void okUsesSuccessCode() {
        ApiResponse<String> response = ApiResponse.ok("ready");

        assertThat(response.getCode()).isEqualTo("0");
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo("ready");
    }

    @Test
    void pageResultReplacesNullRecords() {
        PageResult<String> page = PageResult.of(1, 20, 0, null);

        assertThat(page.getRecords()).isEmpty();
        assertThat(page.getTotal()).isZero();
    }

    @Test
    void pageResultCopiesRecords() {
        PageResult<String> page = PageResult.of(2, 10, 1, List.of("a"));

        assertThat(page.getPageNum()).isEqualTo(2);
        assertThat(page.getRecords()).containsExactly("a");
    }
}
