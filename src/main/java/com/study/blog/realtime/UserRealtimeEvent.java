package com.study.blog.realtime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRealtimeEvent {
    private String eventType;
    private Object payload;
}
