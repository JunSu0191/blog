package com.study.blog.notification;

public record PushSendResult(boolean success, boolean invalidateSubscription) {

    public static PushSendResult sent() {
        return new PushSendResult(true, false);
    }

    public static PushSendResult invalidated() {
        return new PushSendResult(false, true);
    }

    public static PushSendResult retryable() {
        return new PushSendResult(false, false);
    }
}
