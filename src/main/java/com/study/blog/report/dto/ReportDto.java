package com.study.blog.report.dto;

import com.study.blog.report.ModerationAction;
import com.study.blog.report.ReportStatus;
import com.study.blog.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ReportDto {

    private ReportDto() {
    }

    public record CreateRequest(
            @NotNull ReportTargetType targetType,
            Long postId,
            Long commentId,
            @NotBlank @Size(max = 100) String reason,
            @Size(max = 1000) String description
    ) {
    }

    public record ResolveRequest(
            @Size(max = 1000) String note,
            ModerationAction action
    ) {
    }

    public record Response(
            Long id,
            ReportTargetType targetType,
            Long postId,
            Long commentId,
            String reason,
            String description,
            ReportStatus status,
            String reporterUsername,
            String targetTitle,
            String targetPreview,
            String resolutionNote,
            String resolvedByUsername,
            LocalDateTime resolvedAt,
            LocalDateTime createdAt
    ) {
    }

    public record ModerationPostResponse(
            Long postId,
            String title,
            Long authorId,
            String authorUsername,
            long openReportCount,
            LocalDateTime latestReportedAt,
            boolean hidden
    ) {
    }

    public record ModerationCommentResponse(
            Long commentId,
            Long postId,
            String postTitle,
            Long authorId,
            String authorUsername,
            String content,
            long openReportCount,
            LocalDateTime latestReportedAt,
            boolean hidden
    ) {
    }
}
