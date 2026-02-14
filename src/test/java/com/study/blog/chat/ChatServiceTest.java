package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatConversationRepository conversationRepository;
    @Mock
    private ChatConversationMemberRepository conversationMemberRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;
    @Mock
    private NotificationService notificationService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                conversationMemberRepository,
                messageRepository,
                userRepository,
                realtimeEventPublisher,
                notificationService);
    }

    @Test
    void directConversationShouldNotBeDuplicatedByDirectKey() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User other = User.builder().id(2L).username("u2").name("U2").build();

        ChatConversation conversation = ChatConversation.builder()
                .id(10L)
                .type(ConversationType.DIRECT)
                .directKey("1:2")
                .createdBy(requester)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(conversationRepository.findByDirectKey("1:2"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(10L, 1L)).thenReturn(true);
        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(conversationRepository.save(any(ChatConversation.class))).thenReturn(conversation);

        ChatDto.CreateConversationRequest req = new ChatDto.CreateConversationRequest();
        req.setType(ConversationType.DIRECT);
        req.setOtherUserId(2L);

        ChatDto.ConversationSummaryResponse first = chatService.createConversation(1L, req);
        ChatDto.ConversationSummaryResponse second = chatService.createConversation(1L, req);

        assertThat(first.getConversationId()).isEqualTo(second.getConversationId());
        verify(conversationRepository, times(1)).save(any(ChatConversation.class));
    }

    @Test
    void sendMessageShouldReturnExistingMessageForSameClientMsgId() {
        Long conversationId = 11L;
        Long senderId = 1L;

        User sender = User.builder().id(senderId).username("u1").name("U1").build();
        ChatConversation conversation = ChatConversation.builder().id(conversationId).type(ConversationType.GROUP).build();
        ChatMessage existing = ChatMessage.builder()
                .id(100L)
                .conversation(conversation)
                .sender(sender)
                .clientMsgId("1c133edb-8d25-4d97-bad2-b154326890f2")
                .type("TEXT")
                .body("hello")
                .metadata(Map.of("k", "v"))
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, senderId)).thenReturn(true);
        when(messageRepository.findByConversation_IdAndSender_IdAndClientMsgId(conversationId, senderId, existing.getClientMsgId()))
                .thenReturn(Optional.of(existing));

        ChatDto.SendMessageRequest req = new ChatDto.SendMessageRequest();
        req.setClientMsgId(existing.getClientMsgId());
        req.setType("TEXT");
        req.setBody("hello");

        ChatService.SendResult result = chatService.sendMessage(senderId, conversationId, req);

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.message().getId()).isEqualTo(100L);
        verify(messageRepository, never()).save(any(ChatMessage.class));
        verify(notificationService, never()).createChatMessageNotifications(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    void listConversationsShouldIncludeUnreadMessageCount() {
        ConversationSummaryProjection row = mock(ConversationSummaryProjection.class);
        LocalDateTime now = LocalDateTime.now();

        when(row.getConversationId()).thenReturn(20L);
        when(row.getConversationType()).thenReturn("GROUP");
        when(row.getTitle()).thenReturn("팀 채팅");
        when(row.getDirectKey()).thenReturn(null);
        when(row.getLastMessageId()).thenReturn(301L);
        when(row.getLastMessageBody()).thenReturn("last");
        when(row.getLastMessageCreatedAt()).thenReturn(now);
        when(row.getLastSenderId()).thenReturn(7L);
        when(row.getUnreadMessageCount()).thenReturn(4L);
        when(conversationRepository.findConversationSummariesByUserId(1L)).thenReturn(List.of(row));

        List<ChatDto.ConversationSummaryResponse> responses = chatService.listConversations(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUnreadMessageCount()).isEqualTo(4L);
        assertThat(responses.get(0).getLastActivityAt()).isEqualTo(now);
    }

    @Test
    void markAsReadShouldPublishUnreadCountEvent() {
        Long conversationId = 44L;
        Long userId = 2L;
        Long lastReadMessageId = 99L;

        ChatConversation conversation = ChatConversation.builder().id(conversationId).build();
        ChatMessage lastRead = ChatMessage.builder()
                .id(lastReadMessageId)
                .conversation(conversation)
                .build();
        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, userId))
                .conversation(conversation)
                .build();

        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(true);
        when(messageRepository.findByIdAndConversation_Id(lastReadMessageId, conversationId)).thenReturn(Optional.of(lastRead));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(3L);

        chatService.markAsRead(userId, conversationId, lastReadMessageId);

        assertThat(member.getLastReadMessageId()).isEqualTo(lastReadMessageId);
        assertThat(member.getLastReadAt()).isNotNull();

        ArgumentCaptor<ChatDto.ConversationUnreadCountEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatDto.ConversationUnreadCountEvent.class);
        verify(realtimeEventPublisher).publishConversationUnreadCount(eq(userId), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo("CONVERSATION_UNREAD_COUNT_UPDATED");
        assertThat(eventCaptor.getValue().getConversationId()).isEqualTo(conversationId);
        assertThat(eventCaptor.getValue().getUnreadMessageCount()).isEqualTo(0L);
        assertThat(eventCaptor.getValue().getTotalUnreadMessageCount()).isEqualTo(3L);
    }

    @Test
    void leaveConversationShouldDeleteMembershipWhenMembersRemain() {
        Long conversationId = 50L;
        Long userId = 3L;

        ChatConversation conversation = ChatConversation.builder()
                .id(conversationId)
                .type(ConversationType.GROUP)
                .build();
        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, userId))
                .conversation(conversation)
                .build();

        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.countByConversation_Id(conversationId)).thenReturn(2L);
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(6L);

        chatService.leaveConversation(userId, conversationId);

        verify(conversationMemberRepository).delete(member);
        verify(messageRepository, never()).clearReplyReferences(anyLong());
        verify(messageRepository, never()).deleteByConversation_Id(anyLong());
        verify(conversationRepository, never()).delete(any(ChatConversation.class));
        verify(realtimeEventPublisher).publishConversationUnreadCount(eq(userId), any(ChatDto.ConversationUnreadCountEvent.class));
    }

    @Test
    void leaveConversationShouldCleanupConversationWhenLastMemberLeaves() {
        Long conversationId = 51L;
        Long userId = 4L;

        ChatConversation conversation = ChatConversation.builder()
                .id(conversationId)
                .type(ConversationType.GROUP)
                .build();
        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, userId))
                .conversation(conversation)
                .build();

        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.countByConversation_Id(conversationId)).thenReturn(0L);
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(1L);

        chatService.leaveConversation(userId, conversationId);

        verify(conversationMemberRepository).delete(member);
        verify(messageRepository).clearReplyReferences(conversationId);
        verify(messageRepository).deleteByConversation_Id(conversationId);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void createDirectConversationShouldRestoreMissingMemberOnExistingConversation() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User other = User.builder().id(2L).username("u2").name("U2").build();

        ChatConversation conversation = ChatConversation.builder()
                .id(70L)
                .type(ConversationType.DIRECT)
                .directKey("1:2")
                .createdBy(other)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(conversationRepository.findByDirectKey("1:2")).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(70L, 1L)).thenReturn(false);
        when(conversationMemberRepository.existsByConversation_IdAndUser_Id(70L, 2L)).thenReturn(true);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(requester));

        ChatDto.ConversationSummaryResponse response = chatService.createDirectConversation(1L, 2L);

        assertThat(response.getConversationId()).isEqualTo(70L);
        verify(conversationMemberRepository).saveAll(argThat(members -> {
            int count = 0;
            ChatConversationMember only = null;
            for (ChatConversationMember member : members) {
                count++;
                only = member;
            }
            return count == 1 && only != null && Long.valueOf(1L).equals(only.getUser().getId());
        }));
        verify(conversationRepository, never()).save(any(ChatConversation.class));
    }
}
