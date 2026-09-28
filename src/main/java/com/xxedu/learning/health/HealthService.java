package com.xxedu.learning.health;

import com.xxedu.learning.common.enums.HealthStatus;
import com.xxedu.learning.common.log.BizLogger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public final class HealthService {

    private final String applicationName;

    public HealthService(Environment environment) {
        this.applicationName = environment.getRequiredProperty("spring.application.name");
    }

    public HealthVO current() {
        BizLogger.info("health.check", "application={}", applicationName);
        return new HealthVO(HealthStatus.UP);
    }
}
