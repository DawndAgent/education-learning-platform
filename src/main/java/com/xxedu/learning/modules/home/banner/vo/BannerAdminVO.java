package com.xxedu.learning.modules.home.banner.vo;

import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerAdminVO {

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private HomeLinkType linkType;
    private Long linkId;
    private String linkUrl;
    private String targetTitle;
    private Integer sort;
    private HomeItemStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
