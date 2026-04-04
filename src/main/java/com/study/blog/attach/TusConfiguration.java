package com.study.blog.attach;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class TusConfiguration {

    public static final String DEFAULT_TUS_STORAGE_DIR = "./upload/tus";

    @Bean
    public TusFileUploadService tusFileUploadService(
            @Value("${app.tus.storage-path:" + DEFAULT_TUS_STORAGE_DIR + "}") String tusStoragePath) throws IOException {
        Path uploadPath = Paths.get(tusStoragePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        return new TusFileUploadService()
                .withUploadUri("/api/attach-files/uploads")
                .withStoragePath(uploadPath.toString())
                .withDownloadFeature()
                .withUploadExpirationPeriod(86400000L);
    }
}
