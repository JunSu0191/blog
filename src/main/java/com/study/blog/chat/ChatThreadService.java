package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.dto.ChatDto;
import com.study.blog.chat.social.FriendshipService;
import com.study.blog.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatThreadService {

    private final ChatService chatService;
    private final FriendshipService friendshipService;
    private final UserRepository userRepository;

    public ChatThreadService(ChatService chatService,
                             FriendshipService friendshipService,
                             UserRepository userRepository) {
        this.chatService = chatService;
        this.friendshipService = friendshipService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatContractDto.ThreadSummaryResponse> listThreads(Long userId, ConversationType type) {
        return chatService.listConversations(userId, type).stream()
                .map(this::toThreadSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatContractDto.ThreadParticipantsResponse listThreadParticipants(Long userId, Long threadId) {
        ChatDto.ConversationParticipantsResponse source = chatService.listConversationParticipants(userId, threadId);
        ChatContractDto.ThreadParticipantsResponse response = new ChatContractDto.ThreadParticipantsResponse();
        response.setParticipantCount(source.getParticipantCount());
        response.setParticipants(source.getParticipants());
        return response;
    }

    public void hideThread(Long userId, Long threadId) {
        chatService.hideConversation(userId, threadId);
    }

    public void unhideThread(Long userId, Long threadId) {
        chatService.unhideConversation(userId, threadId);
    }

    public void clearMyMessages(Long userId, Long threadId) {
        chatService.clearMyConversationMessages(userId, threadId);
    }

    public void leaveGroup(Long userId, Long groupId) {
        chatService.leaveGroupConversation(userId, groupId);
    }

    public ChatContractDto.ThreadSummaryResponse startDirect(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("본인과의 DIRECT 대화는 생성할 수 없습니다.");
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new IllegalArgumentException("대화 상대를 찾을 수 없습니다.");
        }
        if (!friendshipService.isFriends(userId, targetUserId)) {
            throw new IllegalStateException("친구 관계에서만 DIRECT 대화를 시작할 수 있습니다.");
        }
        friendshipService.ensureNotBlocked(userId, targetUserId);

        ChatDto.ConversationSummaryResponse created = chatService.createDirectConversation(userId, targetUserId);
        chatService.unhideConversation(userId, created.getConversationId());
        return toThreadSummary(created);
    }

    private ChatContractDto.ThreadSummaryResponse toThreadSummary(ChatDto.ConversationSummaryResponse source) {
        ChatContractDto.ThreadSummaryResponse response = new ChatContractDto.ThreadSummaryResponse();
        response.setThreadId(source.getConversationId());
        response.setType(source.getType());
        response.setTitle(source.getTitle());
        response.setDisplayTitle(source.getDisplayTitle());
        response.setAvatarUrl(source.getAvatarUrl());
        response.setDirectKey(source.getDirectKey());
        response.setLastMessage(source.getLastMessage());
        response.setLastActivityAt(source.getLastActivityAt());
        response.setUnreadMessageCount(source.getUnreadMessageCount());
        response.setParticipantCount(source.getParticipantCount());
        response.setParticipants(source.getParticipants());
        response.setHidden(source.isHidden());
        return response;
    }
}
