package com.study.blog.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class WebPushPayload {
    private String title;
    private String body;
    private String tag;
    private String linkUrl;
    private String type;
    private Map<String, Object> data;
    private String icon;
    private String badge;
}
