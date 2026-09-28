package com.xxedu.learning.health;

import com.xxedu.learning.common.constant.ApiConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiStartupTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthApiStarts() {
        ResponseEntity<String> response = restTemplate.getForEntity(ApiConstants.HEALTH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
        assertThat(response.getBody()).contains("\"code\":\"0\"");
        assertThat(response.getBody()).contains("\"message\":\"success\"");
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
