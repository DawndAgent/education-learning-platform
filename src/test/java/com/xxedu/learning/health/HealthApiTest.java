package com.xxedu.learning.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.common.constant.SecurityConstants;
import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthApiTest extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void snowflakeLongSerializesAsString() throws Exception {
        String json = objectMapper.writeValueAsString(new IdSample(9007199254740993L, 10L));

        assertThat(json).contains("\"id\":\"9007199254740993\"");
        assertThat(json).contains("\"timestamp\":10");
    }

    @Test
    void healthMatchesContract() throws Exception {
        mockMvc.perform(get(ApiConstants.HEALTH))
                .andExpect(status().isOk())
                .andExpect(header().exists(SecurityConstants.TRACE_HEADER))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void openApiIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("XX教育学习中心 API"))
                .andExpect(jsonPath("$.paths['/api/health']").exists());
    }

    private record IdSample(Long id, long timestamp) {
    }
}
