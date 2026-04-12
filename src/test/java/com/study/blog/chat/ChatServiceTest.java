package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.chat.social.FriendshipService;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.user.UserAvatarService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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
    private FriendshipService friendshipService;
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserAvatarService userAvatarService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                conversationMemberRepository,
                messageRepository,
                userRepository,
                friendshipService,
                realtimeEventPublisher,
                notificationService,
                userAvatarService);
        lenient().when(userAvatarService.getAvatarUrls(anyCollection())).thenReturn(Map.of());
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
        when(friendshipService.isFriends(1L, 2L)).thenReturn(true);
        when(conversationRepository.findByDirectKey("1:2"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(ChatConversationMember.builder()
                        .id(new ChatConversationMemberId(10L, 1L))
                        .conversation(conversation)
                        .user(requester)
                        .build()));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(ChatConversationMember.builder()
                        .id(new ChatConversationMemberId(10L, 2L))
                        .conversation(conversation)
                        .user(other)
                        .build()));
        ConversationMemberNameProjection directMe = memberName(10L, 1L, "U1");
        ConversationMemberNameProjection directOther = memberName(10L, 2L, "U2");
        when(conversationMemberRepository.findMemberNamesByConversationId(10L))
                .thenReturn(List.of(directMe, directOther));
        when(conversationRepository.save(any(ChatConversation.class))).thenReturn(conversation);

        ChatDto.CreateConversationRequest req = new ChatDto.CreateConversationRequest();
        req.setType(ConversationType.DIRECT);
        req.setOtherUserId(2L);

        ChatDto.ConversationSummaryResponse first = chatService.createConversation(1L, req);
        ChatDto.ConversationSummaryResponse second = chatService.createConversation(1L, req);

        assertThat(first.getConversationId()).isEqualTo(second.getConversationId());
        assertThat(first.getDisplayTitle()).isEqualTo("U2");
        assertThat(first.getAvatarUrl()).isNull();
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

        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, senderId)).thenReturn(true);
        when(messageRepository.findByConversation_IdAndSender_IdAndClientMsgId(conversationId, senderId, existing.getClientMsgId()))
                .thenReturn(Optional.of(existing));

        ChatDto.SendMessageRequest req = new ChatDto.SendMessageRequest();
        req.setClientMsgId(existing.getClientMsgId());
        req.setType("TEXT");
        req.setBody("hello");

        ChatService.SendResult result = chatService.sendMessage(senderId, conversationId, req);

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.message().getId()).isEqualTo(100L);
        assertThat(result.message().getSenderAvatarUrl()).isNull();
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
        when(row.getHiddenAt()).thenReturn(null);
        when(conversationRepository.findConversationSummariesByUserId(1L, null)).thenReturn(List.of(row));
        ConversationMemberNameProjection groupMe = memberName(20L, 1L, "나");
        ConversationMemberNameProjection groupMember = memberName(20L, 2L, "팀원");
        when(conversationMemberRepository.findMemberNamesByConversationIds(List.of(20L)))
                .thenReturn(List.of(groupMe, groupMember));
        when(userAvatarService.getAvatarUrls(anyCollection())).thenReturn(Map.of(7L, "https://cdn.example.com/u7.png"));

        List<ChatDto.ConversationSummaryResponse> responses = chatService.listConversations(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUnreadMessageCount()).isEqualTo(4L);
        assertThat(responses.get(0).getLastActivityAt()).isEqualTo(now);
        assertThat(responses.get(0).getDisplayTitle()).isEqualTo("팀 채팅");
        assertThat(responses.get(0).getLastMessage().getSenderAvatarUrl()).isEqualTo("https://cdn.example.com/u7.png");
    }

    @Test
    void listChatUsersShouldIncludeAvatarUrl() {
        User me = User.builder().id(1L).username("u1").name("U1").nickname("me").deletedYn("N").build();
        User other = User.builder().id(2L).username("u2").name("U2").nickname("other").deletedYn("N").build();
        when(userRepository.findByDeletedYnOrderByIdAsc("N")).thenReturn(List.of(me, other));
        when(userAvatarService.getAvatarUrls(anyCollection())).thenReturn(Map.of(2L, "https://cdn.example.com/u2.png"));

        List<ChatDto.ChatUserResponse> responses = chatService.listChatUsers(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).isMe()).isTrue();
        assertThat(responses.get(1).getAvatarUrl()).isEqualTo("https://cdn.example.com/u2.png");
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

        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, userId)).thenReturn(true);
        when(messageRepository.findByIdAndConversation_Id(lastReadMessageId, conversationId)).thenReturn(Optional.of(lastRead));
        when(conversationMemberRepository.findActiveByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(3L);

        chatService.markAsRead(userId, conversationId, lastReadMessageId);

        assertThat(member.getLastReadMessageId()).isEqualTo(lastReadMessageId);
        assertThat(member.getLastReadAt()).isNotNull();

        ArgumentCaptor<ChatDto.ConversationUnreadCountEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatDto.ConversationUnreadCountEvent.class);
        verify(realtimeEventPublisher).publishConversationUnreadCount(eq(userId), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getConversationId()).isEqualTo(conversationId);
        assertThat(eventCaptor.getValue().getUnreadMessageCount()).isEqualTo(0L);
        assertThat(eventCaptor.getValue().getTotalUnreadMessageCount()).isEqualTo(3L);
    }

    @Test
    void leaveConversationForGroupShouldSetLeftAndHidden() {
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

        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findActiveByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(6L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).username("u3").build()));

        chatService.leaveConversation(userId, conversationId);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(member.getHiddenAt()).isNotNull();
        verify(conversationMemberRepository, never()).delete(any(ChatConversationMember.class));
    }

    @Test
    void leaveConversationForDirectShouldThrow() {
        Long conversationId = 77L;
        Long userId = 3L;

        ChatConversation conversation = ChatConversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> chatService.leaveConversation(userId, conversationId));
        assertThat(ex.getMessage()).contains("DIRECT 대화는 나가기를 지원하지 않습니다");
    }

    @Test
    void hideConversationShouldUpdateHiddenAt() {
        Long conversationId = 81L;
        Long userId = 5L;

        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, userId))
                .build();

        when(conversationMemberRepository.findActiveByConversationIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(member));
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(2L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(10L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).username("u5").build()));

        chatService.hideConversation(userId, conversationId);

        assertThat(member.getHiddenAt()).isNotNull();
        verify(realtimeEventPublisher).publishConversationUnreadCount(eq(userId), any(ChatDto.ConversationUnreadCountEvent.class));
    }

    @Test
    void clearMyConversationMessagesShouldSetClearCursor() {
        Long conversationId = 82L;
        Long userId = 6L;

        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, userId))
                .lastReadMessageId(123L)
                .build();

        when(conversationMemberRepository.findActiveByConversationIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(member));
        when(conversationMemberRepository.countUnreadMessages(conversationId, userId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(userId)).thenReturn(0L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).username("u6").build()));

        chatService.clearMyConversationMessages(userId, conversationId);

        assertThat(member.getLastClearedAt()).isNotNull();
        assertThat(member.getLastReadMessageId()).isNull();
    }

    @Test
    void sendMessageShouldPublishUserEventForParticipants() {
        Long conversationId = 90L;
        Long senderId = 1L;
        Long receiverId = 2L;

        User sender = User.builder().id(senderId).username("u1").name("U1").build();
        User receiver = User.builder().id(receiverId).username("u2").name("U2").build();
        ChatConversation conversation = ChatConversation.builder().id(conversationId).type(ConversationType.DIRECT).build();

        ChatConversationMember senderMember = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, senderId))
                .conversation(conversation)
                .user(sender)
                .build();
        ChatConversationMember receiverMember = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(conversationId, receiverId))
                .conversation(conversation)
                .user(receiver)
                .hiddenAt(LocalDateTime.now())
                .build();

        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, senderId)).thenReturn(true);
        when(messageRepository.findByConversation_IdAndSender_IdAndClientMsgId(conversationId, senderId, "c1"))
                .thenReturn(Optional.empty());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.findByConversationId(conversationId)).thenReturn(List.of(senderMember, receiverMember));
        when(friendshipService.isBlockedBetween(senderId, receiverId)).thenReturn(false);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));

        ChatMessage saved = ChatMessage.builder()
                .id(500L)
                .conversation(conversation)
                .sender(sender)
                .clientMsgId("c1")
                .type("TEXT")
                .body("hello")
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        when(conversationMemberRepository.findUserIdsByConversationId(conversationId)).thenReturn(List.of(senderId, receiverId));
        when(conversationMemberRepository.countUnreadMessages(conversationId, receiverId)).thenReturn(1L);
        when(conversationMemberRepository.countTotalUnreadMessages(receiverId)).thenReturn(1L);
        when(conversationMemberRepository.countUnreadMessages(conversationId, senderId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(senderId)).thenReturn(0L);

        ChatDto.SendMessageRequest req = new ChatDto.SendMessageRequest();
        req.setClientMsgId("c1");
        req.setType("TEXT");
        req.setBody("hello");

        chatService.sendMessage(senderId, conversationId, req);

        verify(realtimeEventPublisher, atLeast(1))
                .publishUserEvent(eq("u2"), eq(receiverId), eq("chat.message.created"), any());
    }

    @Test
    void getMessagesShouldDenyWhenNotMember() {
        when(conversationMemberRepository.findActiveByConversationIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,
                () -> chatService.getMessages(1L, 1L, null, 20));
    }

    @Test
    void leaveGroupConversationShouldFanoutToOthersOnly() {
        Long groupId = 120L;
        Long leaverId = 10L;
        Long otherId = 11L;

        ChatConversation group = ChatConversation.builder()
                .id(groupId)
                .type(ConversationType.GROUP)
                .build();
        ChatConversationMember member = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(groupId, leaverId))
                .conversation(group)
                .user(User.builder().id(leaverId).username("u10").build())
                .build();

        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(groupId, leaverId)).thenReturn(true);
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(conversationMemberRepository.findActiveByConversationIdAndUserId(groupId, leaverId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.findUserIdsByConversationId(groupId)).thenReturn(List.of(leaverId, otherId));
        when(conversationMemberRepository.countUnreadMessages(groupId, leaverId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(leaverId)).thenReturn(0L);
        when(conversationMemberRepository.countUnreadMessages(groupId, otherId)).thenReturn(0L);
        when(conversationMemberRepository.countTotalUnreadMessages(otherId)).thenReturn(0L);
        when(userRepository.findById(leaverId)).thenReturn(Optional.of(User.builder().id(leaverId).username("u10").build()));
        when(userRepository.findById(otherId)).thenReturn(Optional.of(User.builder().id(otherId).username("u11").build()));

        chatService.leaveGroupConversation(leaverId, groupId);

        verify(realtimeEventPublisher).publishUserEvent(eq("u11"), eq(otherId), eq("chat.group.member.left"), any());
        verify(realtimeEventPublisher).publishUserEvent(eq("u11"), eq(otherId), eq("chat.group.membership.updated"), any());
        verify(realtimeEventPublisher, never()).publishUserEvent(eq("u10"), eq(leaverId), eq("chat.group.member.left"), any());
    }

    private ConversationMemberNameProjection memberName(Long conversationId, Long userId, String userName) {
        ConversationMemberNameProjection projection = mock(ConversationMemberNameProjection.class);
        lenient().when(projection.getConversationId()).thenReturn(conversationId);
        lenient().when(projection.getUserId()).thenReturn(userId);
        lenient().when(projection.getUserName()).thenReturn(userName);
        return projection;
    }
}
