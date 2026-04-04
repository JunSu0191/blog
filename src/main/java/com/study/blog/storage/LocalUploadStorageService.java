package com.study.blog.storage;

import com.study.blog.core.web.PublicUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalUploadStorageService implements UploadStorageService {

    private final Path storageRoot;
    private final PublicUrlBuilder publicUrlBuilder;

    public LocalUploadStorageService(@Value("${app.storage.local-root:./upload}") String storageRoot,
                                     PublicUrlBuilder publicUrlBuilder) {
        this.storageRoot = Paths.get(storageRoot).normalize();
        this.publicUrlBuilder = publicUrlBuilder;
    }

    @Override
    public String store(String relativePath, InputStream inputStream, String contentType, Long contentLength) throws IOException {
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        Path destination = storageRoot.resolve(normalizedRelativePath).normalize();
        Files.createDirectories(destination.getParent());
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        return "/upload/" + normalizedRelativePath.replace('\\', '/');
    }

    @Override
    public String toPublicUrl(HttpServletRequest request, String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return publicUrlBuilder.build(request, "/");
        }
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
            return storedPath;
        }
        return publicUrlBuilder.build(request, storedPath);
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
}
