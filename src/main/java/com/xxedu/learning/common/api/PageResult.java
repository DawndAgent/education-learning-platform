package com.xxedu.learning.common.api;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表响应。records 必须是 VO，禁止放入 Entity。
 */
@Getter
public class PageResult<T> {

    private final long pageNum;
    private final long pageSize;
    private final long total;

    @Getter(lombok.AccessLevel.NONE)
    private final List<T> records;

    private PageResult(long pageNum, long pageSize, long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = List.copyOf(records);
    }

    public List<T> getRecords() {
        return new ArrayList<>(records);
    }

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records) {
        List<T> safeRecords = records == null ? List.of() : List.copyOf(records);
        return new PageResult<>(pageNum, pageSize, total, safeRecords);
    }
}
