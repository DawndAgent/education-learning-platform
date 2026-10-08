package com.xxedu.learning.modules.wechat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.wechat.miniapp")
public class WechatMiniappProperties {

    /**
     * 小程序 AppID。
     */
    private String appId = "";

    /**
     * 小程序 AppSecret。
     */
    private String appSecret = "";

    /**
     * 小程序码打开的版本：release / trial / develop。
     */
    private String envVersion = "release";

    /**
     * 为 true 时不调用微信接口，生成本地可展示的二维码（仅开发/测试）。
     */
    private boolean mockEnabled = false;

    public boolean credentialsConfigured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    public boolean useMock() {
        return mockEnabled;
    }
}
