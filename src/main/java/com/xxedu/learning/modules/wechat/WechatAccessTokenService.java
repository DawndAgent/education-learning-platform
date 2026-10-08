package com.xxedu.learning.modules.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class WechatAccessTokenService {

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={secret}";

    private final WechatMiniappProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public String getAccessToken() {
        CachedToken current = cache.get();
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return current.token();
        }
        synchronized (this) {
            CachedToken again = cache.get();
            if (again != null && again.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                return again.token();
            }
            String appId = properties.getAppId() == null ? "" : properties.getAppId().trim();
            String secret = properties.getAppSecret() == null ? "" : properties.getAppSecret().trim();
            String body = restClient.get()
                    .uri(TOKEN_URL, appId, secret)
                    .retrieve()
                    .body(String.class);
            try {
                JsonNode node = objectMapper.readTree(body == null ? "{}" : body);
                if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
                    BizLogger.info("wechat.token.fail", "errcode={} errmsg={}",
                            node.path("errcode").asInt(), node.path("errmsg").asText());
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "获取微信 access_token 失败：" + node.path("errmsg").asText("未知错误"));
                }
                String token = node.path("access_token").asText(null);
                int expiresIn = node.path("expires_in").asInt(7200);
                if (token == null || token.isBlank()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取微信 access_token 失败");
                }
                cache.set(new CachedToken(token, Instant.now().plusSeconds(Math.max(60, expiresIn))));
                BizLogger.info("wechat.token.ok", "expiresIn={}", expiresIn);
                return token;
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析微信 access_token 失败");
            }
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
