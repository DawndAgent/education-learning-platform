package com.xxedu.learning.modules.home.banner.dto;

import com.xxedu.learning.common.api.PageQuery;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BannerQueryRequest extends PageQuery {

    @Size(max = 64, message = "关键字长度不能超过64")
    private String keyword;

    private HomeItemStatus status;
}
