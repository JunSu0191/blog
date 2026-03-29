package com.study.blog.report;

import com.study.blog.post.Post;

import java.time.LocalDateTime;

public interface ReportedPostProjection {

    Post getPost();

    long getOpenReportCount();

    LocalDateTime getLatestReportedAt();
}
