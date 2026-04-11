package com.study.blog.notification;

import com.study.blog.notification.channel.NotificationDeliveryChannel;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationDeliveryChannel notificationDeliveryChannel;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                List.of(notificationDeliveryChannel));
    }

    @Test
    void createChatMessageNotificationsShouldExcludeSender() {
        User user2 = User.builder().id(2L).username("u2").name("U2").build();
        User user3 = User.builder().id(3L).username("u3").name("U3").build();

        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user2, user3));
        when(notificationRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createChatMessageNotifications(
                99L,
                1L,
                "sender",
                "message",
                1000L,
                List.of(1L, 2L, 3L));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Long> recipientIds = captor.getValue().stream()
                .map(n -> n.getUser().getId())
                .toList();

        assertThat(recipientIds).containsExactlyInAnyOrder(2L, 3L);
        assertThat(recipientIds).doesNotContain(1L);

        verify(notificationDeliveryChannel, times(2)).deliver(any(Notification.class), any());
    }

    @Test
    void createPostCommentNotificationShouldCreateAndPush() {
        User owner = User.builder().id(10L).username("owner").name("Owner").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(999L);
            return n;
        });

        notificationService.createPostCommentNotification(
                10L,
                2L,
                "commenter",
                100L,
                "post title",
                55L,
                null,
                "hello");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("POST_COMMENT");
        assertThat(saved.getUser().getId()).isEqualTo(10L);
        assertThat(saved.getLinkUrl()).isEqualTo("/posts/100");
        assertThat(saved.getPayload()).containsEntry("postId", 100L);
        assertThat(saved.getPayload()).containsEntry("commentId", 55L);
        assertThat(saved.getPayload()).containsEntry("commenterId", 2L);

        verify(notificationDeliveryChannel).deliver(any(Notification.class), any());
    }

    @Test
    void createPostCommentNotificationShouldSkipSelfComment() {
        notificationService.createPostCommentNotification(
                2L,
                2L,
                "commenter",
                100L,
                "post title",
                55L,
                null,
                "hello");

        verify(userRepository, never()).findById(anyLong());
        verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
        verify(notificationDeliveryChannel, never()).deliver(any(Notification.class), any());
    }

    @Test
    void createFriendRequestReceivedNotificationShouldCreateAndPush() {
        User target = User.builder().id(20L).username("target").name("Target").build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(target));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(111L);
            return n;
        });

        notificationService.createFriendRequestReceivedNotification(
                20L,
                101L,
                12L,
                "홍길동",
                "길동",
                "hong12");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("FRIEND_REQUEST_RECEIVED");
        assertThat(saved.getLinkUrl()).isEqualTo("/chat");
        assertThat(saved.getPayload()).containsEntry("requestId", 101L);
        assertThat(saved.getPayload()).containsEntry("requesterId", 12L);
        assertThat(saved.getPayload()).containsEntry("targetUserId", 20L);
        assertThat(saved.getPayload()).containsEntry("requesterNickname", "길동");
        assertThat(saved.getPayload()).containsEntry("requesterUsername", "hong12");

        verify(notificationDeliveryChannel).deliver(any(Notification.class), any());
    }
}
