package com.xxedu.learning.modules.dashboard.service;

import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentCategoryCountRow;
import com.xxedu.learning.modules.content.mapper.ContentCountRow;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.mapper.ContentRecentRow;
import com.xxedu.learning.modules.dashboard.vo.DashboardCategoryStatVO;
import com.xxedu.learning.modules.dashboard.vo.DashboardOverviewVO;
import com.xxedu.learning.modules.dashboard.vo.DashboardRecentContentVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECENT_LIMIT = 10;

    private final ContentMapper contentMapper;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    @RequirePermission(PermissionCodes.DASHBOARD_VIEW)
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        Map<String, Long> byStatus = toCountMap(contentMapper.countGroupByStatus());
        long published = value(byStatus, ContentStatus.PUBLISHED.name());
        long draft = value(byStatus, ContentStatus.DRAFT.name());
        long offline = value(byStatus, ContentStatus.OFFLINE.name());
        vo.setPublishedCount(published);
        vo.setDraftCount(draft);
        vo.setScheduledPublishCount(contentMapper.countScheduledDrafts());
        vo.setOfflineCount(offline);
        vo.setContentTotal(published + draft + offline);

        Map<String, Long> byType = toCountMap(contentMapper.countGroupByContentType());
        vo.setArticleCount(value(byType, ContentType.ARTICLE.name()));
        vo.setVideoCount(value(byType, ContentType.VIDEO.name()));

        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime nextWeek = weekStart.plusWeeks(1);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonth = monthStart.plusMonths(1);
        vo.setWeekNewCount(contentMapper.countCreatedBetween(weekStart, nextWeek));
        vo.setMonthNewCount(contentMapper.countCreatedBetween(monthStart, nextMonth));

        List<ContentCategoryCountRow> categoryRows = contentMapper.countGroupByRootCategory();
        Map<Long, String> names = categoryService.namesByIds(
                categoryRows.stream().map(ContentCategoryCountRow::getCategoryId).toList());
        List<DashboardCategoryStatVO> categoryStats = new ArrayList<>(categoryRows.size());
        for (ContentCategoryCountRow row : categoryRows) {
            DashboardCategoryStatVO item = new DashboardCategoryStatVO();
            item.setCategoryId(row.getCategoryId());
            item.setCategoryName(names.getOrDefault(row.getCategoryId(), "未命名分类"));
            item.setCount(row.getCount() == null ? 0L : row.getCount());
            categoryStats.add(item);
        }
        vo.setCategoryStats(categoryStats);

        List<ContentRecentRow> recentRows = contentMapper.selectRecentPublished(RECENT_LIMIT);
        List<DashboardRecentContentVO> recent = new ArrayList<>(recentRows.size());
        for (ContentRecentRow row : recentRows) {
            DashboardRecentContentVO item = new DashboardRecentContentVO();
            item.setId(row.getId());
            item.setTitle(row.getTitle());
            item.setContentType(row.getContentType());
            item.setCategoryName(row.getCategoryName());
            item.setPublishTime(row.getPublishTime());
            recent.add(item);
        }
        vo.setRecentPublished(recent);
        return vo;
    }

    private static Map<String, Long> toCountMap(List<ContentCountRow> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (ContentCountRow row : rows) {
            if (row.getKey() == null) {
                continue;
            }
            map.put(row.getKey(), row.getCount() == null ? 0L : row.getCount());
        }
        return map;
    }

    private static long value(Map<String, Long> map, String key) {
        return map.getOrDefault(key, 0L);
    }
}
