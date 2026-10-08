package com.xxedu.learning.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.util.StringUtils;

import java.io.InputStream;

public class CosStorageService implements StorageService {

    private final StorageProperties properties;
    private COSClient cosClient;
    private boolean ownsClient;

    public CosStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    CosStorageService(StorageProperties properties, COSClient cosClient) {
        this.properties = properties;
        this.cosClient = cosClient;
        this.ownsClient = false;
    }

    @PostConstruct
    void init() {
        StorageProperties.Cos cos = properties.getCos();
        requireText(cos.getSecretId(), "storage.cos.secret-id");
        requireText(cos.getSecretKey(), "storage.cos.secret-key");
        requireText(cos.getRegion(), "storage.cos.region");
        requireText(cos.getBucket(), "storage.cos.bucket");
        requireText(cos.getPublicBaseUrl(), "storage.cos.public-base-url");
        if (cosClient == null) {
            COSCredentials credentials = new BasicCOSCredentials(cos.getSecretId().trim(), cos.getSecretKey().trim());
            ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion().trim()));
            cosClient = new COSClient(credentials, clientConfig);
            ownsClient = true;
        }
        BizLogger.info("storage.cos.init", "region={} bucket={} publicBaseUrl={}",
                cos.getRegion().trim(), cos.getBucket().trim(), normalizeBaseUrl(cos.getPublicBaseUrl()));
    }

    @PreDestroy
    void destroy() {
        if (ownsClient && cosClient != null) {
            cosClient.shutdown();
        }
    }

    @Override
    public StorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long size) {
        String key = normalizeObjectKey(objectKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        try {
            PutObjectRequest request = new PutObjectRequest(
                    properties.getCos().getBucket().trim(), key, inputStream, metadata);
            cosClient.putObject(request);
        } catch (CosServiceException ex) {
            BizLogger.info("storage.cos.upload_fail", "key={} status={} code={} msg={}",
                    key, ex.getStatusCode(), ex.getErrorCode(), ex.getErrorMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, cosUploadMessage(ex));
        } catch (CosClientException ex) {
            BizLogger.info("storage.cos.upload_fail", "key={} msg={}", key, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件存储失败");
        }
        return new StorageUploadResult(key, getUrl(key), contentType, size);
    }

    @Override
    public void delete(String objectKey) {
        String key = normalizeObjectKey(objectKey);
        try {
            cosClient.deleteObject(properties.getCos().getBucket().trim(), key);
        } catch (CosClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败");
        }
    }

    @Override
    public String getUrl(String objectKey) {
        String key = normalizeObjectKey(objectKey);
        return normalizeBaseUrl(properties.getCos().getPublicBaseUrl()) + "/" + key;
    }

    static String normalizeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对象键不能为空");
        }
        String normalized = objectKey.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.contains("..") || normalized.startsWith("/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法的对象键");
        }
        return normalized;
    }

    static String normalizeBaseUrl(String publicBaseUrl) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " 不能为空");
        }
    }

    static String cosUploadMessage(CosServiceException ex) {
        String code = ex.getErrorCode() == null ? "" : ex.getErrorCode();
        String msg = ex.getErrorMessage() == null ? "" : ex.getErrorMessage();
        if ("UnavailableForLegalReasons".equals(code) || msg.toLowerCase().contains("arrears")) {
            return "腾讯云 COS 账号欠费，请充值后再上传";
        }
        if (ex.getStatusCode() == 403 || "AccessDenied".equals(code)) {
            return "腾讯云 COS 无写入权限，请检查密钥与桶策略";
        }
        if (ex.getStatusCode() == 404 || "NoSuchBucket".equals(code)) {
            return "腾讯云 COS 存储桶不存在，请检查配置";
        }
        return "文件存储失败";
    }
}
