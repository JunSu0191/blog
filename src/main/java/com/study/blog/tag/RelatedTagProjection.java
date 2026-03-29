package com.study.blog.tag;

public interface RelatedTagProjection {

    Long getId();

    String getName();

    String getSlug();

    long getPostCount();
}
