package com.study.blog.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    @EntityGraph(attributePaths = {
            "post", "post.user", "comment", "comment.user", "reporter", "resolvedBy"
    })
    Page<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select r.post as post,
                   count(r.id) as openReportCount,
                   max(r.createdAt) as latestReportedAt
            from ContentReport r
            where r.targetType = com.study.blog.report.ReportTargetType.POST
              and r.status = com.study.blog.report.ReportStatus.OPEN
              and r.post is not null
            group by r.post
            """)
    Page<ReportedPostProjection> findOpenReportedPosts(Pageable pageable);

    @Query("""
            select r.comment as comment,
                   count(r.id) as openReportCount,
                   max(r.createdAt) as latestReportedAt
            from ContentReport r
            where r.targetType = com.study.blog.report.ReportTargetType.COMMENT
              and r.status = com.study.blog.report.ReportStatus.OPEN
              and r.comment is not null
            group by r.comment
            """)
    Page<ReportedCommentProjection> findOpenReportedComments(Pageable pageable);
}
