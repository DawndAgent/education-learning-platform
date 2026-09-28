package com.xxedu.learning.health;

import com.xxedu.learning.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HealthVO {

    private final HealthStatus status;
}
