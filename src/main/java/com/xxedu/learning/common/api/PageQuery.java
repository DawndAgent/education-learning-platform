package com.xxedu.learning.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 列表请求的分页参数。列表接口必须接收分页，禁止一次返回全量数据。
 */
@Data
public class PageQuery {

    public static final long MAX_PAGE_SIZE = 100L;

    @Min(value = 1, message = "页码最小为1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100")
    private long pageSize = 20;
}
