package com.xxedu.learning.common.exception;

import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest extends IntegrationTestSupport {

    @Test
    void businessExceptionKeepsUnifiedBody() throws Exception {
        mockMvc.perform(get("/api/v1/public/probe/biz-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void validationErrorReturnsFieldMessages() throws Exception {
        mockMvc.perform(post("/api/v1/public/probe/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].message").value("名称不能为空"));
    }

    @Test
    void unknownExceptionDoesNotLeakInternalMessage() throws Exception {
        mockMvc.perform(get("/api/v1/public/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"))
                .andExpect(content().string(not(containsString("secret-token"))));
    }

    @Test
    void missingPublicResourceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/public/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));
    }
}
