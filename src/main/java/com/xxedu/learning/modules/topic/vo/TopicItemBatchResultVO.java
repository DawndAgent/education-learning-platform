package com.xxedu.learning.modules.topic.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicItemBatchResultVO {

    private int successCount;
    private int duplicateCount;
    private int invalidCount;
}
