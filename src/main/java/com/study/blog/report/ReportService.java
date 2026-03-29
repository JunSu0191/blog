package com.study.blog.report;

import com.study.blog.comment.Comment;
import com.study.blog.comment.CommentRepository;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.Post;
import com.study.blog.post.PostErrorCode;
import com.study.blog.post.PostRepository;
import com.study.blog.report.dto.ReportDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReportService {

    private final ContentReportRepository contentReportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportService(ContentReportRepository contentReportRepository,
                         PostRepository postRepository,
                         CommentRepository commentRepository,
                         UserRepository userRepository) {
        this.contentReportRepository = contentReportRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public ReportDto.Response createReport(Long reporterUserId, ReportDto.CreateRequest request) {
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post targetPost = null;
        Comment targetComment = null;
        if (request.targetType() == ReportTargetType.POST) {
            if (request.postId() == null) {
                throw new IllegalArgumentException("신고할 게시글 ID가 필요합니다.");
            }
            targetPost = postRepository.findWithAssociationsById(request.postId())
                    .orElseThrow(() -> new CodedApiException(
                            PostErrorCode.POST_NOT_FOUND.code(),
                            HttpStatus.NOT_FOUND,
                            "게시글을 찾을 수 없습니다."));
        } else {
            if (request.commentId() == null) {
                throw new IllegalArgumentException("신고할 댓글 ID가 필요합니다.");
            }
            targetComment = commentRepository.findById(request.commentId())
                    .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        }

        ContentReport report = ContentReport.builder()
                .targetType(request.targetType())
                .post(targetPost)
                .comment(targetComment)
                .reporter(reporter)
                .reason(request.reason().trim())
                .description(normalizeNullable(request.description()))
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(contentReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public Page<ReportDto.Response> listReports(Pageable pageable) {
        Page<ContentReport> page = contentReportRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<ReportDto.Response> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public ReportDto.Response resolveReport(String actorUsername, Long reportId, ReportDto.ResolveRequest request) {
        User admin = userRepository.findByUsernameAndDeletedYn(actorUsername, "N")
                .orElseThrow(() -> new IllegalArgumentException("관리자 사용자를 찾을 수 없습니다."));
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        ModerationAction action = request.action() == null ? ModerationAction.NONE : request.action();
        applyModerationAction(report, action);

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolutionNote(normalizeNullable(request.note()));
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return toResponse(contentReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public Page<ReportDto.ModerationPostResponse> listModerationPosts(Pageable pageable) {
        Page<ReportedPostProjection> page = contentReportRepository.findOpenReportedPosts(pageable);
        List<ReportDto.ModerationPostResponse> content = page.getContent().stream()
                .map(projection -> new ReportDto.ModerationPostResponse(
                        projection.getPost().getId(),
                        projection.getPost().getTitle(),
                        projection.getPost().getUser().getId(),
                        projection.getPost().getUser().getUsername(),
                        projection.getOpenReportCount(),
                        projection.getLatestReportedAt(),
                        projection.getPost().isDeleted()))
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ReportDto.ModerationCommentResponse> listModerationComments(Pageable pageable) {
        Page<ReportedCommentProjection> page = contentReportRepository.findOpenReportedComments(pageable);
        List<ReportDto.ModerationCommentResponse> content = page.getContent().stream()
                .map(projection -> new ReportDto.ModerationCommentResponse(
                        projection.getComment().getId(),
                        projection.getComment().getPost().getId(),
                        projection.getComment().getPost().getTitle(),
                        projection.getComment().getUser().getId(),
                        projection.getComment().getUser().getUsername(),
                        projection.getComment().getContent(),
                        projection.getOpenReportCount(),
                        projection.getLatestReportedAt(),
                        projection.getComment().isDeleted()))
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private void applyModerationAction(ContentReport report, ModerationAction action) {
        if (action == ModerationAction.NONE) {
            return;
        }

        if (report.getTargetType() == ReportTargetType.POST && report.getPost() != null) {
            Post post = report.getPost();
            if (action == ModerationAction.HIDE_TARGET) {
                post.setDeletedYn("Y");
                post.setDeletedAt(LocalDateTime.now());
            } else if (action == ModerationAction.RESTORE_TARGET) {
                post.setDeletedYn("N");
                post.setDeletedAt(null);
            }
            postRepository.save(post);
            return;
        }

        if (report.getTargetType() == ReportTargetType.COMMENT && report.getComment() != null) {
            Comment comment = report.getComment();
            if (action == ModerationAction.HIDE_TARGET) {
                comment.setDeletedYn("Y");
                comment.setDeletedAt(LocalDateTime.now());
            } else if (action == ModerationAction.RESTORE_TARGET) {
                comment.setDeletedYn("N");
                comment.setDeletedAt(null);
            }
            comment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }

    private ReportDto.Response toResponse(ContentReport report) {
        String targetTitle = null;
        String targetPreview = null;
        Long postId = null;
        Long commentId = null;
        if (report.getPost() != null) {
            postId = report.getPost().getId();
            targetTitle = report.getPost().getTitle();
            targetPreview = report.getPost().getExcerpt();
        } else if (report.getComment() != null) {
            commentId = report.getComment().getId();
            postId = report.getComment().getPost() == null ? null : report.getComment().getPost().getId();
            targetTitle = report.getComment().getPost() == null ? null : report.getComment().getPost().getTitle();
            targetPreview = report.getComment().getContent();
        }

        return new ReportDto.Response(
                report.getId(),
                report.getTargetType(),
                postId,
                commentId,
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getReporter() == null ? null : report.getReporter().getUsername(),
                targetTitle,
                targetPreview,
                report.getResolutionNote(),
                report.getResolvedBy() == null ? null : report.getResolvedBy().getUsername(),
                report.getResolvedAt(),
                report.getCreatedAt());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
