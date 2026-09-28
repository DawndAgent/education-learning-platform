package com.xxedu.learning.modules.video.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VideoSourceType {

    WECHAT_CHANNEL("WECHAT_CHANNEL"),
    TENCENT_VIDEO("TENCENT_VIDEO");

    @EnumValue
    private final String code;
}
