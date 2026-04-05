package com.study.blog.chat.social;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.realtime.UserEventType;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final FriendshipRequestRepository friendshipRequestRepository;
    private final UserRepository userRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             FriendshipRequestRepository friendshipRequestRepository,
                             UserRepository userRepository,
                             RealtimeEventPublisher realtimeEventPublisher) {
        this.friendshipRepository = friendshipRepository;
        this.friendshipRequestRepository = friendshipRequestRepository;
        this.userRepository = userRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ChatContractDto.FriendResponse> listFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        if (friendships.isEmpty()) {
            return List.of();
        }
        List<Long> friendIds = friendships.stream()
                .map(friendship -> friendship.getFriendUser().getId())
                .distinct()
                .toList();
        Map<Long, User> friendById = userRepository.findAllById(friendIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendById.get(friendship.getFriendUser().getId());
                    if (friend == null) {
                        return null;
                    }
                    ChatContractDto.FriendResponse response = new ChatContractDto.FriendResponse();
                    response.setUserId(friend.getId());
                    response.setUsername(friend.getUsername());
                    response.setName(friend.getName());
                    response.setNickname(friend.getNickname());
                    response.setFriendshipCreatedAt(friendship.getCreatedAt());
                    return response;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatContractDto.FriendRequestResponse> listRequests(Long userId, FriendRequestListType type) {
        List<FriendshipRequest> requests;
        if (type == FriendRequestListType.RECEIVED) {
            requests = friendshipRequestRepository.findReceivedByUserAndStatus(userId, FriendshipRequestStatus.PENDING);
        } else {
            requests = friendshipRequestRepository.findSentByUserAndStatus(userId, FriendshipRequestStatus.PENDING);
        }
        return requests.stream().map(this::toFriendRequestResponse).toList();
    }

    public ChatContractDto.FriendRequestResponse sendRequest(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }
        User requester = getUserOrThrow(requesterId);
        User target = getUserOrThrow(targetId);
        ensureNotBlocked(requesterId, targetId);
        if (isFriends(requesterId, targetId)) {
            throw new IllegalStateException("이미 친구입니다.");
        }

        Optional<FriendshipRequest> samePending = friendshipRequestRepository
                .findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
                requesterId,
                targetId,
                FriendshipRequestStatus.PENDING);
        if (samePending.isPresent()) {
            return toFriendRequestResponse(samePending.get());
        }

        Optional<FriendshipRequest> reversePending = friendshipRequestRepository
                .findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
                targetId,
                requesterId,
                FriendshipRequestStatus.PENDING);
        if (reversePending.isPresent()) {
            throw new IllegalStateException("상대방의 친구 요청이 이미 도착해 있습니다.");
        }

        FriendshipRequest created = friendshipRequestRepository.save(FriendshipRequest.builder()
                .requester(requester)
                .target(target)
                .status(FriendshipRequestStatus.PENDING)
                .build());

        ChatContractDto.FriendRequestResponse response = toFriendRequestResponse(created);
        publishUserEvent(target, UserEventType.FRIEND_REQUEST_CREATED, Map.of("request", response));
        publishUserEvent(requester, UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        return response;
    }

    public ChatContractDto.FriendRequestResponse acceptRequest(Long userId, Long requestId) {
        FriendshipRequest request = friendshipRequestRepository.findById(requestId)
                .orElseThrow(this::friendRequestNotFound);
        if (!request.getTarget().getId().equals(userId)) {
            throw friendRequestForbidden();
        }
        if (request.getStatus() == FriendshipRequestStatus.ACCEPTED) {
            return toFriendRequestResponse(request);
        }
        if (request.getStatus() != FriendshipRequestStatus.PENDING) {
            throw friendRequestNotPending();
        }

        Long requesterId = request.getRequester().getId();
        ensureNotBlocked(userId, requesterId);

        removeHistoricalRequestStatusDuplicates(request, FriendshipRequestStatus.ACCEPTED);
        request.setStatus(FriendshipRequestStatus.ACCEPTED);
        createFriendshipPair(request.getRequester(), request.getTarget());

        ChatContractDto.FriendRequestResponse response = toFriendRequestResponse(request);
        publishUserEvent(request.getRequester(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        publishUserEvent(request.getTarget(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        return response;
    }

    public ChatContractDto.FriendRequestResponse rejectRequest(Long userId, Long requestId) {
        FriendshipRequest request = friendshipRequestRepository.findById(requestId)
                .orElseThrow(this::friendRequestNotFound);
        if (!request.getTarget().getId().equals(userId)) {
            throw friendRequestForbidden();
        }
        if (request.getStatus() == FriendshipRequestStatus.REJECTED) {
            return toFriendRequestResponse(request);
        }
        if (request.getStatus() != FriendshipRequestStatus.PENDING) {
            throw friendRequestNotPending();
        }

        removeHistoricalRequestStatusDuplicates(request, FriendshipRequestStatus.REJECTED);
        request.setStatus(FriendshipRequestStatus.REJECTED);
        ChatContractDto.FriendRequestResponse response = toFriendRequestResponse(request);
        publishUserEvent(request.getRequester(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        publishUserEvent(request.getTarget(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        return response;
    }

    public ChatContractDto.FriendRequestCancelResponse cancelRequest(Long userId, Long requestId) {
        FriendshipRequest request = friendshipRequestRepository.findById(requestId)
                .orElseThrow(this::friendRequestNotFound);
        if (!request.getRequester().getId().equals(userId)) {
            throw friendRequestForbidden();
        }
        if (request.getStatus() != FriendshipRequestStatus.PENDING) {
            throw friendRequestNotPending();
        }

        LocalDateTime canceledAt = LocalDateTime.now();
        removeHistoricalRequestStatusDuplicates(request, FriendshipRequestStatus.CANCELED);
        request.setStatus(FriendshipRequestStatus.CANCELED);
        request.setUpdatedAt(canceledAt);

        ChatContractDto.FriendRequestResponse updated = toFriendRequestResponse(request);
        publishUserEvent(request.getRequester(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", updated));
        publishUserEvent(request.getTarget(), UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", updated));

        ChatContractDto.FriendRequestCancelResponse response = new ChatContractDto.FriendRequestCancelResponse();
        response.setRequestId(request.getId());
        response.setStatus(request.getStatus());
        response.setCanceledAt(canceledAt);
        return response;
    }

    public void removeFriend(Long userId, Long friendUserId) {
        if (Objects.equals(userId, friendUserId)) {
            throw new IllegalArgumentException("본인은 친구 삭제 대상이 될 수 없습니다.");
        }
        friendshipRepository.deletePair(userId, friendUserId);
    }

    public ChatContractDto.FriendRequestResponse blockUser(Long userId, Long targetUserId) {
        if (Objects.equals(userId, targetUserId)) {
            throw new IllegalArgumentException("본인은 차단할 수 없습니다.");
        }
        User user = getUserOrThrow(userId);
        User target = getUserOrThrow(targetUserId);

        friendshipRepository.deletePair(userId, targetUserId);
        friendshipRequestRepository.bulkUpdateStatusBetween(
                userId,
                targetUserId,
                FriendshipRequestStatus.PENDING,
                FriendshipRequestStatus.CANCELED);

        FriendshipRequest request = friendshipRequestRepository
                .findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
                        userId,
                        targetUserId,
                        FriendshipRequestStatus.BLOCKED)
                .orElseGet(() -> friendshipRequestRepository.save(FriendshipRequest.builder()
                        .requester(user)
                        .target(target)
                        .status(FriendshipRequestStatus.BLOCKED)
                        .build()));

        ChatContractDto.FriendRequestResponse response = toFriendRequestResponse(request);
        publishUserEvent(user, UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        publishUserEvent(target, UserEventType.FRIEND_REQUEST_UPDATED, Map.of("request", response));
        return response;
    }

    @Transactional(readOnly = true)
    public boolean isFriends(Long userId, Long otherUserId) {
        return friendshipRepository.existsByUser_IdAndFriendUser_Id(userId, otherUserId)
                && friendshipRepository.existsByUser_IdAndFriendUser_Id(otherUserId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedBetween(Long userA, Long userB) {
        return friendshipRequestRepository.existsBlockedRelation(
                userA,
                userB,
                FriendshipRequestStatus.BLOCKED);
    }

    @Transactional(readOnly = true)
    public void ensureNotBlocked(Long userA, Long userB) {
        if (isBlockedBetween(userA, userB)) {
            throw new IllegalStateException("차단 상태에서는 요청할 수 없습니다.");
        }
    }

    private void removeHistoricalRequestStatusDuplicates(FriendshipRequest request, FriendshipRequestStatus status) {
        friendshipRequestRepository.deleteByRequesterTargetStatusExcludingId(
                request.getRequester().getId(),
                request.getTarget().getId(),
                status,
                request.getId());
    }

    private void createFriendshipPair(User requester, User target) {
        saveFriendshipIfAbsent(requester, target);
        saveFriendshipIfAbsent(target, requester);
    }

    private void saveFriendshipIfAbsent(User user, User friendUser) {
        if (friendshipRepository.existsByUser_IdAndFriendUser_Id(user.getId(), friendUser.getId())) {
            return;
        }
        try {
            friendshipRepository.save(Friendship.builder()
                    .user(user)
                    .friendUser(friendUser)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // concurrent accept에서도 unique key를 기준으로 멱등 처리한다.
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ChatContractDto.FriendRequestResponse toFriendRequestResponse(FriendshipRequest request) {
        ChatContractDto.FriendRequestResponse response = new ChatContractDto.FriendRequestResponse();
        response.setId(request.getId());
        response.setRequesterId(request.getRequester().getId());
        response.setRequesterUsername(request.getRequester().getUsername());
        response.setRequesterName(request.getRequester().getName());
        response.setRequesterNickname(request.getRequester().getNickname());
        response.setTargetId(request.getTarget().getId());
        response.setTargetUsername(request.getTarget().getUsername());
        response.setTargetName(request.getTarget().getName());
        response.setTargetNickname(request.getTarget().getNickname());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }

    private void publishUserEvent(User user, UserEventType eventType, Object payload) {
        realtimeEventPublisher.publishUserEvent(user.getUsername(), user.getId(), eventType.value(), payload);
    }

    private CodedApiException friendRequestNotFound() {
        return new CodedApiException(
                "friend_request_not_found",
                HttpStatus.NOT_FOUND,
                "친구 요청을 찾을 수 없습니다.");
    }

    private CodedApiException friendRequestForbidden() {
        return new CodedApiException(
                "friend_request_forbidden",
                HttpStatus.FORBIDDEN,
                "요청 권한이 없습니다.");
    }

    private CodedApiException friendRequestNotPending() {
        return new CodedApiException(
                "friend_request_not_pending",
                HttpStatus.CONFLICT,
                "대기 상태의 친구 요청만 처리할 수 있습니다.");
    }
}
