package com.study.blog.report;

import com.study.blog.comment.Comment;

import java.time.LocalDateTime;

public interface ReportedCommentProjection {

    Comment getComment();

    long getOpenReportCount();

    LocalDateTime getLatestReportedAt();
}
