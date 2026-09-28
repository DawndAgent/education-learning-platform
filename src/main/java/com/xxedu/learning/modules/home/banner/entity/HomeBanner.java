package com.xxedu.learning.modules.home.banner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_banner")
public class HomeBanner extends BaseEntity {

    private String title;
    private String subtitle;
    private String imageUrl;
    private HomeLinkType linkType;
    private Long linkId;
    private String linkUrl;
    private Integer sort;
    private HomeItemStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
