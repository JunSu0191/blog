package com.study.blog.attach;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class TusConfiguration {

    public static final String FINAL_UPLOAD_DIR = "./upload";

    @Bean
    public TusFileUploadService tusFileUploadService() throws IOException {
        Path uploadPath = Paths.get(FINAL_UPLOAD_DIR);
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
