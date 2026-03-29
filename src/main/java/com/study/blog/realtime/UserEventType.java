package com.study.blog.realtime;

public enum UserEventType {
    CHAT_MESSAGE_CREATED("chat.message.created"),
    CHAT_THREAD_UPDATED("chat.thread.updated"),
    FRIEND_REQUEST_CREATED("friend.request.created"),
    FRIEND_REQUEST_UPDATED("friend.request.updated"),
    GROUP_INVITE_CREATED("group.invite.created"),
    GROUP_INVITE_UPDATED("group.invite.updated"),
    CHAT_GROUP_MEMBER_LEFT("chat.group.member.left"),
    CHAT_GROUP_MEMBERSHIP_UPDATED("chat.group.membership.updated");

    private final String value;

    UserEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
