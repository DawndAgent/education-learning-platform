package com.xxedu.learning.modules.content.dto;

import com.xxedu.learning.common.api.PageQuery;
import com.xxedu.learning.modules.content.enums.ContentScheduleFilter;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContentQueryRequest extends PageQuery {

    private Long categoryId;
    private ContentType contentType;
    private ContentStatus status;

    @Size(max = 64, message = "标题关键字长度不能超过64")
    private String keyword;

    /**
     * 可选排序。publishTime = 按发布时间倒序（首页最新 / 搜索）。
     * 其他或空值保持分类排序优先。
     */
    @Size(max = 32, message = "排序参数长度不能超过32")
    private String sort;

    /**
     * 普通草稿或待定时发布。与 status 组合使用。
     */
    private ContentScheduleFilter schedule;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTimeFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTimeTo;
}
