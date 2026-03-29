package com.study.blog.blogprofile.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

public final class BlogSettingsDto {

    private BlogSettingsDto() {
    }

    public record Response(
            String themePreset,
            String accentColor,
            String coverImageUrl,
            String profileLayout,
            String fontScale,
            Boolean showStats
    ) {
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 20)
        private String themePreset;
        @Size(max = 7)
        private String accentColor;
        @Size(max = 5000)
        private String coverImageUrl;
        @Size(max = 20)
        private String profileLayout;
        @Size(max = 10)
        private String fontScale;
        private Boolean showStats;
    }
}
