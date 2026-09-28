package com.xxedu.learning.modules.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DashboardOverviewVO {

    private long contentTotal;
    private long publishedCount;
    private long draftCount;
    private long scheduledPublishCount;
    private long offlineCount;
    private long articleCount;
    private long videoCount;
    private long weekNewCount;
    private long monthNewCount;
    private List<DashboardCategoryStatVO> categoryStats = new ArrayList<>();
    private List<DashboardRecentContentVO> recentPublished = new ArrayList<>();
}
