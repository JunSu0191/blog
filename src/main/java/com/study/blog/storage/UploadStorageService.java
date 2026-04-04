package com.study.blog.storage;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;

public interface UploadStorageService {

    String store(String relativePath, InputStream inputStream, String contentType, Long contentLength) throws IOException;

    String toPublicUrl(HttpServletRequest request, String storedPath);
}
