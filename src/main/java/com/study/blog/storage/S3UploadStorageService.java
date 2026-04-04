package com.study.blog.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3UploadStorageService implements UploadStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;
    private final String publicBaseUrl;

    public S3UploadStorageService(S3Client s3Client,
                                  @Value("${app.storage.s3.bucket}") String bucket,
                                  @Value("${app.storage.s3.key-prefix:}") String keyPrefix,
                                  @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = trimSlashes(keyPrefix);
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
    }

    @Override
    public String store(String relativePath, InputStream inputStream, String contentType, Long contentLength) throws IOException {
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        String objectKey = buildObjectKey(normalizedRelativePath);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey);

        if (StringUtils.hasText(contentType)) {
            requestBuilder.contentType(contentType);
        }

        RequestBody requestBody = contentLength != null
                ? RequestBody.fromInputStream(inputStream, contentLength)
                : RequestBody.fromContentProvider(() -> inputStream, contentType);

        s3Client.putObject(requestBuilder.build(), requestBody);
        return buildPublicUrl(objectKey);
    }

    @Override
    public String toPublicUrl(HttpServletRequest request, String storedPath) {
        return storedPath;
    }

    private String buildObjectKey(String normalizedRelativePath) {
        if (!StringUtils.hasText(keyPrefix)) {
            return normalizedRelativePath;
        }
        return keyPrefix + "/" + normalizedRelativePath;
    }

    private String buildPublicUrl(String objectKey) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + objectKey;
        }
        return "https://" + bucket + ".s3.amazonaws.com/" + objectKey;
    }

    private String normalizeRelativePath(String relativePath) {
        String normalized = relativePath == null ? "" : relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        return normalized;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
