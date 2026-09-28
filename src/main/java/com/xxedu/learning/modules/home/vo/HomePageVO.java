package com.xxedu.learning.modules.home.vo;

import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import com.xxedu.learning.modules.home.enums.RecommendType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HomePageVO {

    private List<Banner> banners = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Recommendation> recommendations = new ArrayList<>();
    private List<Recommendation> topics = new ArrayList<>();
    private List<Latest> latestContents = new ArrayList<>();

    @Getter
    @Setter
    public static class Banner {
        private Long id;
        private String title;
        private String subtitle;
        private String imageUrl;
        private HomeLinkType linkType;
        private Long linkId;
        private String linkUrl;
    }

    @Getter
    @Setter
    public static class Category {
        private Long id;
        private String name;
        private String code;
        private String iconUrl;
    }

    @Getter
    @Setter
    public static class Recommendation {
        private Long id;
        private RecommendType type;
        private Long targetId;
        private String title;
        private String coverUrl;
        private String summary;
        private ContentType contentType;
    }

    @Getter
    @Setter
    public static class Latest {
        private Long id;
        private String title;
        private ContentType contentType;
        private String coverUrl;
        private String summary;
        private LocalDateTime publishTime;
    }
}
