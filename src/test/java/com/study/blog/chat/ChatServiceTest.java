package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
}
