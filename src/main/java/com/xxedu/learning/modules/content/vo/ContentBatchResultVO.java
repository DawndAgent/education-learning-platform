package com.xxedu.learning.modules.content.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentBatchResultVO {

    private int successCount;
    private int failedCount;
}
