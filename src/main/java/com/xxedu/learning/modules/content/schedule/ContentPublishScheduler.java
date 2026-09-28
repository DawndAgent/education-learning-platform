package com.xxedu.learning.modules.content.schedule;

import com.xxedu.learning.modules.content.service.ContentPublishJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "content.publish.scheduler-enabled", havingValue = "true")
public class ContentPublishScheduler {

    private final ContentPublishJob contentPublishJob;

    @Scheduled(fixedDelayString = "${content.publish.interval-ms:30000}")
    public void publishDueContents() {
        contentPublishJob.runOnce();
    }
}
