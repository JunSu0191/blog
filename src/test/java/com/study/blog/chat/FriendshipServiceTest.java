package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.social.*;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private FriendshipRequestRepository friendshipRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;
    @Mock
    private NotificationService notificationService;

    private FriendshipService friendshipService;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(
                friendshipRepository,
                friendshipRequestRepository,
                userRepository,
                realtimeEventPublisher,
                notificationService);
    }

    @Test
    void sendRequestShouldCreatePendingRequest() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User target = User.builder().id(2L).username("u2").name("U2").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(friendshipRequestRepository.existsBlockedRelation(1L, 2L, FriendshipRequestStatus.BLOCKED)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriendUser_Id(1L, 2L)).thenReturn(false);
        when(friendshipRequestRepository.findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
                1L, 2L, FriendshipRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(friendshipRequestRepository.findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
                2L, 1L, FriendshipRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        FriendshipRequest saved = FriendshipRequest.builder()
                .id(100L)
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.save(any(FriendshipRequest.class))).thenReturn(saved);

        ChatContractDto.FriendRequestResponse response = friendshipService.sendRequest(1L, 2L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(FriendshipRequestStatus.PENDING);
        verify(realtimeEventPublisher).publishUserEvent(eq("u2"), eq(2L), eq("friend.request.created"), any());
        verify(notificationService).createFriendRequestReceivedNotification(2L, 100L, 1L, "U1", null, "u1");
    }

    @Test
    void acceptRequestShouldCreateBidirectionalFriendship() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User target = User.builder().id(2L).username("u2").name("U2").build();

        FriendshipRequest request = FriendshipRequest.builder()
                .id(200L)
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build();

        when(friendshipRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(friendshipRequestRepository.existsBlockedRelation(2L, 1L, FriendshipRequestStatus.BLOCKED)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriendUser_Id(1L, 2L)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriendUser_Id(2L, 1L)).thenReturn(false);

        ChatContractDto.FriendRequestResponse response = friendshipService.acceptRequest(2L, 200L);

        assertThat(response.getStatus()).isEqualTo(FriendshipRequestStatus.ACCEPTED);
        verify(friendshipRepository, times(2)).save(any(Friendship.class));
        verify(friendshipRequestRepository).deleteByRequesterTargetStatusExcludingId(
                1L, 2L, FriendshipRequestStatus.ACCEPTED, 200L);
        verify(realtimeEventPublisher, atLeastOnce()).publishUserEvent(any(), any(), eq("friend.request.updated"), any());
        verify(notificationService).createFriendRequestAcceptedNotification(1L, 200L, 1L, 2L, "U2", null, "u2");
    }

    @Test
    void rejectRequestShouldRemoveHistoricalRejectedDuplicate() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User target = User.builder().id(2L).username("u2").name("U2").build();

        FriendshipRequest request = FriendshipRequest.builder()
                .id(301L)
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.findById(301L)).thenReturn(Optional.of(request));

        ChatContractDto.FriendRequestResponse response = friendshipService.rejectRequest(2L, 301L);

        assertThat(response.getStatus()).isEqualTo(FriendshipRequestStatus.REJECTED);
        verify(friendshipRequestRepository).deleteByRequesterTargetStatusExcludingId(
                1L, 2L, FriendshipRequestStatus.REJECTED, 301L);
        verify(notificationService).createFriendRequestRejectedNotification(1L, 301L, 1L, 2L, "U2", null, "u2");
    }

    @Test
    void rejectRequestShouldFailWhenTargetDoesNotMatch() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User target = User.builder().id(2L).username("u2").name("U2").build();

        FriendshipRequest request = FriendshipRequest.builder()
                .id(300L)
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.findById(300L)).thenReturn(Optional.of(request));

        CodedApiException ex = assertThrows(CodedApiException.class,
                () -> friendshipService.rejectRequest(3L, 300L));
        assertThat(ex.getCode()).isEqualTo("friend_request_forbidden");
    }

    @Test
    void listRequestsShouldReturnPendingOnly() {
        FriendshipRequest pending = FriendshipRequest.builder()
                .id(1L)
                .requester(User.builder().id(1L).build())
                .target(User.builder().id(2L).build())
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.findReceivedByUserAndStatus(2L, FriendshipRequestStatus.PENDING))
                .thenReturn(List.of(pending));

        List<ChatContractDto.FriendRequestResponse> responses = friendshipService.listRequests(2L, FriendRequestListType.RECEIVED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(FriendshipRequestStatus.PENDING);
    }

    @Test
    void cancelRequestShouldAllowRequesterWhenPending() {
        User requester = User.builder().id(1L).username("u1").name("U1").build();
        User target = User.builder().id(2L).username("u2").name("U2").build();
        FriendshipRequest request = FriendshipRequest.builder()
                .id(400L)
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.findById(400L)).thenReturn(Optional.of(request));

        ChatContractDto.FriendRequestCancelResponse response = friendshipService.cancelRequest(1L, 400L);

        assertThat(response.getRequestId()).isEqualTo(400L);
        assertThat(response.getStatus()).isEqualTo(FriendshipRequestStatus.CANCELED);
        assertThat(response.getCanceledAt()).isNotNull();
        verify(friendshipRequestRepository).deleteByRequesterTargetStatusExcludingId(
                1L, 2L, FriendshipRequestStatus.CANCELED, 400L);
        verify(realtimeEventPublisher, atLeastOnce()).publishUserEvent(any(), any(), eq("friend.request.updated"), any());
        verify(notificationService).createFriendRequestCanceledNotification(2L, 400L, 1L, "U1", null, "u1");
    }

    @Test
    void cancelRequestShouldFailWhenRequesterDoesNotMatch() {
        FriendshipRequest request = FriendshipRequest.builder()
                .id(401L)
                .requester(User.builder().id(1L).build())
                .target(User.builder().id(2L).build())
                .status(FriendshipRequestStatus.PENDING)
                .build();
        when(friendshipRequestRepository.findById(401L)).thenReturn(Optional.of(request));

        CodedApiException ex = assertThrows(CodedApiException.class,
                () -> friendshipService.cancelRequest(2L, 401L));
        assertThat(ex.getCode()).isEqualTo("friend_request_forbidden");
    }

    @Test
    void cancelRequestShouldFailWhenNotPending() {
        FriendshipRequest request = FriendshipRequest.builder()
                .id(402L)
                .requester(User.builder().id(1L).build())
                .target(User.builder().id(2L).build())
                .status(FriendshipRequestStatus.ACCEPTED)
                .build();
        when(friendshipRequestRepository.findById(402L)).thenReturn(Optional.of(request));

        CodedApiException ex = assertThrows(CodedApiException.class,
                () -> friendshipService.cancelRequest(1L, 402L));
        assertThat(ex.getCode()).isEqualTo("friend_request_not_pending");
    }

    @Test
    void cancelRequestShouldFailWhenNotFound() {
        when(friendshipRequestRepository.findById(999L)).thenReturn(Optional.empty());

        CodedApiException ex = assertThrows(CodedApiException.class,
                () -> friendshipService.cancelRequest(1L, 999L));
        assertThat(ex.getCode()).isEqualTo("friend_request_not_found");
        assertThat(ex.getStatus().value()).isEqualTo(404);
    }
}
