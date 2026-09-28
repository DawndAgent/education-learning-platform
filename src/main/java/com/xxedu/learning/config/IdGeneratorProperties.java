package com.xxedu.learning.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.id-generator")
public class IdGeneratorProperties {

    @Min(0)
    @Max(31)
    private long workerId = 1;

    @Min(0)
    @Max(31)
    private long datacenterId = 1;
}
