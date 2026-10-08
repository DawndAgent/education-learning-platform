package com.xxedu.learning.modules.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用 JDK HttpClient 调用微信接口（与 curl 行为一致）。
 * Spring RestClient 默认 Accept/重编码会导致微信返回 HTTP 412 空 body。
 */
@RequiredArgsConstructor
public class WechatMiniProgramCodeClient implements MiniProgramCodeClient {

    private static final String WXACODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    private final WechatAccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public byte[] createUnlimitedCode(String scene, String page, String envVersion) {
        String token = accessTokenService.getAccessToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scene", scene);
        body.put("page", page);
        body.put("check_path", false);
        body.put("env_version", envVersion == null || envVersion.isBlank() ? "release" : envVersion);
        body.put("width", 430);

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成小程序码失败：请求编码错误");
        }

        String url = WXACODE_URL + "?access_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            BizLogger.info("wechat.wxacode.exception", "message={}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成小程序码失败：网络错误");
        }

        byte[] bytes = response.body() == null ? new byte[0] : response.body();
        int status = response.statusCode();
        if (status >= 400) {
            String text = new String(bytes, StandardCharsets.UTF_8).strip();
            BizLogger.info("wechat.wxacode.http_fail", "status={} body={}", status, text);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "生成小程序码失败：微信 HTTP " + status
                            + (text.isBlank() ? "" : (" " + text)));
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "微信小程序码返回为空");
        }
        if (isPng(bytes) || isJpeg(bytes)) {
            BizLogger.info("wechat.wxacode.ok", "scene={} page={} bytes={} format={}",
                    scene, page, bytes.length, isPng(bytes) ? "png" : "jpeg");
            return bytes;
        }
        try {
            JsonNode node = objectMapper.readTree(bytes);
            int errcode = node.path("errcode").asInt();
            String errmsg = node.path("errmsg").asText("未知错误");
            BizLogger.info("wechat.wxacode.fail", "errcode={} errmsg={}", errcode, errmsg);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成小程序码失败：" + errmsg);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成小程序码失败：返回无法识别的内容");
        }
    }

    static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
    }

    static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }
}
