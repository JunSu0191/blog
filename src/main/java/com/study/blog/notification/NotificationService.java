package com.study.blog.notification;

import com.study.blog.notification.channel.NotificationDeliveryChannel;
import com.study.blog.notification.dto.NotificationDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
/**
 * 인앱 알림 서비스.
 *
 * - 알림 목록/읽음 처리
 * - 채팅 메시지 발생 시 수신자 알림 생성
 * - 내 게시글 댓글 발생 시 작성자 알림 생성
 * - 저장 후 실시간 푸시
 */
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationDeliveryChannel> deliveryChannels;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               List<NotificationDeliveryChannel> deliveryChannels) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.deliveryChannels = deliveryChannels;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.Response> list(Long userId, Long cursorId, int size) {
        // size는 과도한 조회를 막기 위해 1~100으로 제한
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return notificationRepository.findByUserIdWithCursor(userId, cursorId, PageRequest.of(0, normalizedSize))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationDto.Response read(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 보안: 본인 알림만 읽음 처리 가능
        if (!notification.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("내 알림만 읽을 수 있습니다.");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return toResponse(notification);
    }

    public int readAll(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createChatMessageNotifications(Long conversationId,
                                               Long senderId,
                                               String senderName,
                                               String messageBody,
                                               Long messageId,
                                               Collection<Long> participantUserIds) {
        // 발신자를 제외한 수신자 집합 생성
        Set<Long> recipients = participantUserIds.stream()
                .filter(userId -> !userId.equals(senderId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (recipients.isEmpty()) {
            return;
        }

        List<User> users = userRepository.findAllById(recipients);
        Map<Long, User> userById = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<Notification> toSave = new ArrayList<>();
        for (Long recipientId : recipients) {
            User user = userById.get(recipientId);
            if (user == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", conversationId);
            payload.put("messageId", messageId);
            payload.put("senderId", senderId);
            payload.put("senderName", senderName);

            toSave.add(buildNotification(
                    user,
                    NotificationType.CHAT_MESSAGE,
                    "새 채팅 메시지",
                    buildChatBody(senderName, messageBody),
                    "/chat?conversationId=" + conversationId,
                    payload));
        }

        List<Notification> saved = notificationRepository.saveAll(toSave);
        notificationRepository.flush();
        // 커밋 이후에만 실시간 이벤트 전달(유령 이벤트 방지)
        runAfterCommitOrNow(() -> saved.forEach(this::dispatchNotification));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPostCommentNotification(Long postOwnerId,
                                              Long commenterId,
                                              String commenterName,
                                              Long postId,
                                              String postTitle,
                                              Long commentId,
                                              Long parentCommentId,
                                              String commentContent) {
        if (postOwnerId == null || commenterId == null || postOwnerId.equals(commenterId)) {
            return;
        }

        userRepository.findById(postOwnerId).ifPresent(postOwner -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("postId", postId);
            payload.put("commentId", commentId);
            payload.put("commenterId", commenterId);
            if (parentCommentId != null) {
                payload.put("parentCommentId", parentCommentId);
            }
            payload.put("commenterName", commenterName);

            Notification saved = notificationRepository.saveAndFlush(buildNotification(
                    postOwner,
                    NotificationType.POST_COMMENT,
                    "내 게시글에 새 댓글",
                    buildPostCommentBody(commenterName, postTitle, commentContent),
                    "/posts/" + postId,
                    payload));
            runAfterCommitOrNow(() -> dispatchNotification(saved));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFriendRequestReceivedNotification(Long targetUserId,
                                                        Long requestId,
                                                        Long requesterId,
                                                        String requesterName,
                                                        String requesterNickname,
                                                        String requesterUsername) {
        createFriendRequestNotification(
                targetUserId,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "친구 요청",
                buildFriendRequestBody(requesterName, requesterNickname, "친구 요청을 보냈습니다."),
                requestId,
                requesterId,
                targetUserId,
                requesterName,
                requesterNickname,
                requesterUsername);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFriendRequestAcceptedNotification(Long requesterUserId,
                                                        Long requestId,
                                                        Long requesterId,
                                                        Long targetUserId,
                                                        String targetName,
                                                        String targetNickname,
                                                        String targetUsername) {
        createFriendRequestNotification(
                requesterUserId,
                NotificationType.FRIEND_REQUEST_ACCEPTED,
                "친구 요청 수락",
                buildFriendRequestBody(targetName, targetNickname, "친구 요청을 수락했습니다."),
                requestId,
                requesterId,
                targetUserId,
                targetName,
                targetNickname,
                targetUsername);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFriendRequestRejectedNotification(Long requesterUserId,
                                                        Long requestId,
                                                        Long requesterId,
                                                        Long targetUserId,
                                                        String targetName,
                                                        String targetNickname,
                                                        String targetUsername) {
        createFriendRequestNotification(
                requesterUserId,
                NotificationType.FRIEND_REQUEST_REJECTED,
                "친구 요청 거절",
                buildFriendRequestBody(targetName, targetNickname, "친구 요청을 거절했습니다."),
                requestId,
                requesterId,
                targetUserId,
                targetName,
                targetNickname,
                targetUsername);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFriendRequestCanceledNotification(Long targetUserId,
                                                        Long requestId,
                                                        Long requesterId,
                                                        String requesterName,
                                                        String requesterNickname,
                                                        String requesterUsername) {
        createFriendRequestNotification(
                targetUserId,
                NotificationType.FRIEND_REQUEST_CANCELED,
                "친구 요청 취소",
                buildFriendRequestBody(requesterName, requesterNickname, "보낸 친구 요청을 취소했습니다."),
                requestId,
                requesterId,
                targetUserId,
                requesterName,
                requesterNickname,
                requesterUsername);
    }

    private void createFriendRequestNotification(Long recipientUserId,
                                                 NotificationType type,
                                                 String title,
                                                 String body,
                                                 Long requestId,
                                                 Long requesterId,
                                                 Long targetUserId,
                                                 String actorName,
                                                 String actorNickname,
                                                 String actorUsername) {
        userRepository.findById(recipientUserId).ifPresent(recipient -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("requestId", requestId);
            payload.put("requesterId", requesterId);
            payload.put("targetUserId", targetUserId);
            payload.put("status", type.name().replace("FRIEND_REQUEST_", ""));
            if (actorNickname != null && !actorNickname.isBlank()) {
                payload.put("requesterNickname", actorNickname);
            }
            if (actorName != null && !actorName.isBlank()) {
                payload.put("requesterName", actorName);
            }
            if (actorUsername != null && !actorUsername.isBlank()) {
                payload.put("requesterUsername", actorUsername);
            }

            Notification saved = notificationRepository.saveAndFlush(buildNotification(
                    recipient,
                    type,
                    title,
                    body,
                    "/chat",
                    payload));
            runAfterCommitOrNow(() -> dispatchNotification(saved));
        });
    }

    private String buildChatBody(String senderName, String messageBody) {
        String prefix = senderName != null ? senderName + ": " : "";
        if (messageBody == null) {
            return prefix + "(첨부/이벤트 메시지)";
        }
        if (messageBody.length() <= 120) {
            return prefix + messageBody;
        }
        return prefix + messageBody.substring(0, 117) + "...";
    }

    private String buildPostCommentBody(String commenterName, String postTitle, String commentContent) {
        String actor = (commenterName == null || commenterName.isBlank()) ? "누군가" : commenterName;
        String safePostTitle = (postTitle == null || postTitle.isBlank())
                ? "내 게시글"
                : "'" + abbreviate(postTitle, 40) + "'";
        String commentPreview = (commentContent == null || commentContent.isBlank())
                ? "댓글이 도착했습니다."
                : abbreviate(commentContent.replace("\n", " ").trim(), 80);
        return actor + "님이 " + safePostTitle + "에 댓글을 남겼습니다: " + commentPreview;
    }

    private String buildFriendRequestBody(String actorName, String actorNickname, String action) {
        String actor = (actorNickname != null && !actorNickname.isBlank())
                ? actorNickname
                : ((actorName != null && !actorName.isBlank()) ? actorName : "누군가");
        return actor + "님이 " + action;
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }

    private NotificationDto.Event buildEvent(Notification notification) {
        NotificationDto.Response data = toResponse(notification);
        NotificationDto.Event event = new NotificationDto.Event();
        event.setType("NOTIFICATION_CREATED");
        event.setNotification(data);
        return event;
    }

    private void dispatchNotification(Notification notification) {
        NotificationDto.Event event = buildEvent(notification);
        for (NotificationDeliveryChannel deliveryChannel : deliveryChannels) {
            try {
                deliveryChannel.deliver(notification, event);
            } catch (Exception ex) {
                log.warn("Failed to deliver notification via channel={}",
                        deliveryChannel.getClass().getSimpleName(), ex);
            }
        }
    }

    private NotificationDto.Response toResponse(Notification n) {
        NotificationDto.Response r = new NotificationDto.Response();
        r.setId(n.getId());
        r.setUserId(n.getUser().getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setBody(n.getBody());
        r.setLinkUrl(n.getLinkUrl());
        r.setPayload(n.getPayload());
        r.setCreatedAt(n.getCreatedAt());
        r.setReadAt(n.getReadAt());
        r.setRead(n.getReadAt() != null);
        r.setArchivedAt(n.getArchivedAt());
        return r;
    }

    private Notification buildNotification(User user,
                                           NotificationType type,
                                           String title,
                                           String body,
                                           String linkUrl,
                                           Map<String, Object> payload) {
        return Notification.builder()
                .user(user)
                .type(type.name())
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .payload(payload)
                .build();
    }

    private void runAfterCommitOrNow(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
