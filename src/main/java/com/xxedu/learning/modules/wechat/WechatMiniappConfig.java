package com.xxedu.learning.modules.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.log.BizLogger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(WechatMiniappProperties.class)
public class WechatMiniappConfig {

    @Bean
    RestClient wechatRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    WechatAccessTokenService wechatAccessTokenService(WechatMiniappProperties properties,
                                                     RestClient wechatRestClient,
                                                     ObjectMapper objectMapper) {
        return new WechatAccessTokenService(properties, wechatRestClient, objectMapper);
    }

    @Bean
    MiniProgramCodeClient miniProgramCodeClient(WechatMiniappProperties properties,
                                                WechatAccessTokenService accessTokenService,
                                                ObjectMapper objectMapper) {
        if (properties.isMockEnabled()) {
            BizLogger.info("wechat.wxacode.client", "mode=mock");
            return new MockMiniProgramCodeClient();
        }
        BizLogger.info("wechat.wxacode.client", "mode=wechat appId={}", properties.getAppId());
        return new WechatMiniProgramCodeClient(accessTokenService, objectMapper);
    }
}
