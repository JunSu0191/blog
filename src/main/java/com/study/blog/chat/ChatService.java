package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.chat.social.FriendshipService;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.realtime.UserEventType;
import com.study.blog.user.UserAvatarService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
/**
 * 채팅 핵심 비즈니스 서비스.
 *
 * 핵심 책임:
 * - 대화방 생성(DIRECT/GROUP)
 * - 메시지 저장/조회/읽음 처리
 * - 메시지 멱등성(clientMsgId) 보장
 * - 트랜잭션 커밋 이후 실시간 이벤트/알림 전파
 */
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final NotificationService notificationService;
    private final UserAvatarService userAvatarService;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatConversationMemberRepository conversationMemberRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       FriendshipService friendshipService,
                       RealtimeEventPublisher realtimeEventPublisher,
                       NotificationService notificationService,
                       UserAvatarService userAvatarService) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.notificationService = notificationService;
        this.userAvatarService = userAvatarService;
    }

    public ChatDto.ConversationSummaryResponse createConversation(Long requesterId, ChatDto.CreateConversationRequest req) {
        User requester = getUserOrThrow(requesterId);

        ChatConversation conversation;
        if (req.getType() == ConversationType.DIRECT) {
            if (req.getOtherUserId() == null) {
                throw new IllegalArgumentException("DIRECT 대화는 otherUserId가 필요합니다.");
            }
            Long otherId = req.getOtherUserId();
            if (requesterId.equals(otherId)) {
                throw new IllegalArgumentException("자기 자신과 DIRECT 대화를 생성할 수 없습니다.");
            }
            if (!friendshipService.isFriends(requesterId, otherId)) {
                throw new IllegalStateException("친구 관계에서만 DIRECT 대화를 시작할 수 있습니다.");
            }
            friendshipService.ensureNotBlocked(requesterId, otherId);
            // DIRECT는 항상 "작은ID:큰ID"로 키를 고정해야 중복 방 생성이 방지된다.
            String directKey = buildDirectKey(requesterId, otherId);
            conversation = conversationRepository.findByDirectKey(directKey)
                    .map(existing -> ensureDirectConversationMembers(existing, requesterId, otherId))
                    .orElseGet(() -> createDirectConversation(requester, directKey, otherId));
        } else {
            conversation = createGroupConversation(requester, req.getTitle(), req.getMemberIds());
        }

        return toSummary(conversation, null, requesterId);
    }

    public ChatDto.ConversationSummaryResponse createDirectConversation(Long requesterId, Long otherUserId) {
        ChatDto.CreateConversationRequest req = new ChatDto.CreateConversationRequest();
        req.setType(ConversationType.DIRECT);
        req.setOtherUserId(otherUserId);
        return createConversation(requesterId, req);
    }

    @Transactional(readOnly = true)
    public List<ChatDto.ConversationSummaryResponse> listConversations(Long userId) {
        return listConversations(userId, null);
    }

    @Transactional(readOnly = true)
    public List<ChatDto.ConversationSummaryResponse> listConversations(Long userId, ConversationType type) {
        List<ConversationSummaryProjection> rows = conversationRepository.findConversationSummariesByUserId(
                userId,
                type != null ? type.name() : null);
        Map<Long, List<ConversationParticipantProjection>> participantsByConversationId =
                loadParticipantsForConversations(rows);
        Map<Long, String> avatarUrls = loadConversationAvatarUrls(rows, participantsByConversationId);
        return rows.stream()
                .map(row -> toSummaryFromProjection(
                        row,
                        userId,
                        participantsByConversationId.get(row.getConversationId()),
                        avatarUrls))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatDto.ConversationParticipantsResponse listConversationParticipants(Long userId, Long conversationId) {
        ensureMembership(conversationId, userId);
        List<ConversationParticipantProjection> participants =
                conversationMemberRepository.findParticipantSummariesByConversationId(conversationId);
        Map<Long, String> avatarUrls = userAvatarService.getAvatarUrls(participants.stream()
                .map(ConversationParticipantProjection::getUserId)
                .toList());

        ChatDto.ConversationParticipantsResponse response = new ChatDto.ConversationParticipantsResponse();
        response.setParticipants(toChatUsers(participants, userId, avatarUrls));
        response.setParticipantCount((long) response.getParticipants().size());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatDto.ChatUserResponse> listChatUsers(Long userId) {
        List<User> users = userRepository.findByDeletedYnOrderByIdAsc("N");
        Map<Long, String> avatarUrls = userAvatarService.getAvatarUrls(users.stream().map(User::getId).toList());
        return users.stream()
                .map(user -> {
                    ChatDto.ChatUserResponse r = new ChatDto.ChatUserResponse();
                    r.setUserId(user.getId());
                    r.setUsername(user.getUsername());
                    r.setName(user.getName());
                    r.setNickname(user.getNickname());
                    r.setAvatarUrl(avatarUrls.get(user.getId()));
                    r.setMe(user.getId().equals(userId));
                    return r;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MessageResponse> getMessages(Long userId, Long conversationId, Long cursorMessageId, int size) {
        // 보안: 대화방 멤버가 아니면 메시지 조회 불가
        ChatConversationMember member = getActiveMemberOrThrow(conversationId, userId);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;
        if (cursorMessageId != null) {
            // cursorMessageId의 createdAt + id 조합을 경계로 다음 페이지를 조회한다.
            ChatMessage cursor = messageRepository.findByIdAndConversation_Id(cursorMessageId, conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("cursorMessageId가 유효하지 않습니다."));
            cursorCreatedAt = cursor.getCreatedAt();
            cursorId = cursor.getId();
        }

        List<ChatMessage> messages = messageRepository.findPageByConversationIdWithCursor(
                        conversationId,
                        member.getLastClearedAt(),
                        cursorCreatedAt,
                        cursorId,
                        PageRequest.of(0, normalizedSize));
        Map<Long, String> avatarUrls = userAvatarService.getAvatarUrls(messages.stream()
                .map(message -> message.getSender() != null ? message.getSender().getId() : null)
                .filter(Objects::nonNull)
                .toList());
        return messages.stream()
                .map(message -> toMessageResponse(message, avatarUrls))
                .toList();
    }

    public void markAsRead(Long userId, Long conversationId, Long lastReadMessageId) {
        // 보안: 대화방 멤버만 읽음 처리 가능
        ensureMembership(conversationId, userId);

        ChatMessage lastRead = messageRepository.findByIdAndConversation_Id(lastReadMessageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("lastReadMessageId가 유효하지 않습니다."));

        ChatConversationMember member = getActiveMemberOrThrow(conversationId, userId);

        // conversation_member에 마지막 읽은 메시지 정보를 저장한다.
        member.setLastReadMessageId(lastRead.getId());
        member.setLastReadAt(LocalDateTime.now());

        // 읽음 상태가 반영된 뒤, 본인 미읽음 카운트를 실시간으로 갱신한다.
        runAfterCommitOrNow(() -> publishUnreadCountForUser(conversationId, userId));
    }

    public void leaveConversation(Long userId, Long conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
        if (conversation.getType() == ConversationType.DIRECT) {
            throw new IllegalStateException("DIRECT 대화는 나가기를 지원하지 않습니다.");
        }
        leaveGroupConversation(userId, conversationId);
    }

    public void leaveGroupConversation(Long userId, Long groupId) {
        ensureMembership(groupId, userId);
        ChatConversation conversation = conversationRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("GROUP 대화만 나가기를 지원합니다.");
        }

        ChatConversationMember member = getActiveMemberOrThrow(groupId, userId);
        LocalDateTime leftAt = LocalDateTime.now();
        List<Long> recipients = Optional.ofNullable(conversationMemberRepository.findUserIdsByConversationId(groupId))
                .orElseGet(List::of).stream()
                .filter(targetUserId -> !targetUserId.equals(userId))
                .distinct()
                .toList();

        member.setLeftAt(leftAt);
        member.setHiddenAt(leftAt);

        runAfterCommitOrNow(() -> {
            publishUnreadCountForUser(groupId, userId);
            publishThreadUpdatedForUser(groupId, userId);
            Map<String, Object> leftPayload = Map.of(
                    "groupId", groupId,
                    "userId", userId,
                    "leftAt", leftAt);
            Map<String, Object> membershipPayload = Map.of(
                    "groupId", groupId,
                    "userId", userId,
                    "status", "LEFT",
                    "leftAt", leftAt);
            for (Long recipientId : recipients) {
                publishUserEvent(recipientId, UserEventType.CHAT_GROUP_MEMBER_LEFT.value(), leftPayload);
                publishUserEvent(recipientId, UserEventType.CHAT_GROUP_MEMBERSHIP_UPDATED.value(), membershipPayload);
                publishThreadUpdatedForUser(groupId, recipientId);
            }
        });
    }

    public void hideConversation(Long userId, Long conversationId) {
        ChatConversationMember member = getActiveMemberOrThrow(conversationId, userId);
        member.setHiddenAt(LocalDateTime.now());
        runAfterCommitOrNow(() -> {
            publishUnreadCountForUser(conversationId, userId);
            publishThreadUpdatedForUser(conversationId, userId);
        });
    }

    public void unhideConversation(Long userId, Long conversationId) {
        ChatConversationMember member = getActiveMemberOrThrow(conversationId, userId);
        member.setHiddenAt(null);
        runAfterCommitOrNow(() -> publishThreadUpdatedForUser(conversationId, userId));
    }

    public void clearMyConversationMessages(Long userId, Long conversationId) {
        ChatConversationMember member = getActiveMemberOrThrow(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        member.setLastClearedAt(now);
        member.setLastReadAt(now);
        member.setLastReadMessageId(null);
        runAfterCommitOrNow(() -> {
            publishUnreadCountForUser(conversationId, userId);
            publishThreadUpdatedForUser(conversationId, userId);
        });
    }

    public SendResult sendMessage(Long senderId, Long conversationId, ChatDto.SendMessageRequest req) {
        ensureMembership(conversationId, senderId);

        // 멱등성: 동일 (conversationId, senderId, clientMsgId) 요청은 기존 메시지를 반환
        Optional<ChatMessage> existing = messageRepository.findByConversation_IdAndSender_IdAndClientMsgId(
                conversationId, senderId, req.getClientMsgId());
        if (existing.isPresent()) {
            return new SendResult(toMessageResponse(existing.get(), userAvatarService.getAvatarUrls(List.of(senderId))), true);
        }

        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
        User sender = getUserOrThrow(senderId);
        if (conversation.getType() == ConversationType.DIRECT) {
            Long otherUserId = conversationMemberRepository.findByConversationId(conversationId).stream()
                    .map(member -> member.getUser().getId())
                    .filter(userId -> !userId.equals(senderId))
                    .findFirst()
                    .orElse(null);
            if (otherUserId != null && friendshipService.isBlockedBetween(senderId, otherUserId)) {
                throw new IllegalStateException("차단 상태에서는 메시지를 보낼 수 없습니다.");
            }
        }

        ChatMessage replyTo = null;
        if (req.getReplyToMessageId() != null) {
            replyTo = messageRepository.findByIdAndConversation_Id(req.getReplyToMessageId(), conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("replyToMessageId가 유효하지 않습니다."));
        }

        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .clientMsgId(req.getClientMsgId())
                .type(req.getType())
                .body(req.getBody())
                .metadata(req.getMetadata())
                .replyToMessage(replyTo)
                .build());

        List<Long> participants = conversationMemberRepository.findUserIdsByConversationId(conversationId);
        ChatDto.MessageResponse messageResponse = toMessageResponse(saved, userAvatarService.getAvatarUrls(List.of(senderId)));

        // DB 트랜잭션이 성공적으로 커밋된 뒤에만 실시간/알림을 전송한다.
        // (커밋 전에 보내면 롤백 시 "유령 이벤트"가 생길 수 있음)
        runAfterCommitOrNow(() -> {
            broadcastMessageCreated(conversationId, messageResponse);
            broadcastUnreadCountForRecipients(conversationId, senderId, participants);
            publishMessageCreatedToUserEvents(conversationId, messageResponse, participants);
            notificationService.createChatMessageNotifications(
                    conversationId,
                    senderId,
                    sender.getName(),
                    saved.getBody(),
                    saved.getId(),
                    participants);
            // TODO: add mention(@username) extraction and targeted notification creation.
        });

        return new SendResult(messageResponse, false);
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long conversationId, Long userId) {
        return conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, userId);
    }

    private ChatConversation createDirectConversation(User requester, String directKey, Long otherUserId) {
        User other = getUserOrThrow(otherUserId);

        try {
            ChatConversation conversation = conversationRepository.save(ChatConversation.builder()
                    .type(ConversationType.DIRECT)
                    .directKey(directKey)
                    .createdBy(requester)
                    .build());

            // DIRECT는 두 사용자만 멤버로 저장
            saveMembers(conversation, List.of(requester, other));
            return conversation;
        } catch (DataIntegrityViolationException ex) {
            // 동시 생성 경쟁 상황: 유니크 키 충돌 시 이미 생성된 방을 재조회
            return conversationRepository.findByDirectKey(directKey)
                    .orElseThrow(() -> ex);
        }
    }

    private ChatConversation ensureDirectConversationMembers(ChatConversation conversation, Long requesterId, Long otherUserId) {
        List<Long> missingMemberIds = new ArrayList<>(2);
        Optional<ChatConversationMember> requesterMember =
                conversationMemberRepository.findByConversation_IdAndUser_Id(conversation.getId(), requesterId);
        if (requesterMember.isPresent()) {
            requesterMember.get().setLeftAt(null);
            requesterMember.get().setHiddenAt(null);
        } else {
            missingMemberIds.add(requesterId);
        }

        Optional<ChatConversationMember> otherMember =
                conversationMemberRepository.findByConversation_IdAndUser_Id(conversation.getId(), otherUserId);
        if (otherMember.isPresent()) {
            otherMember.get().setLeftAt(null);
        } else {
            missingMemberIds.add(otherUserId);
        }
        if (missingMemberIds.isEmpty()) {
            return conversation;
        }

        List<User> missingUsers = userRepository.findAllById(missingMemberIds);
        if (missingUsers.size() != missingMemberIds.size()) {
            throw new IllegalArgumentException("DIRECT 대화 멤버 사용자를 찾을 수 없습니다.");
        }

        try {
            saveMembers(conversation, missingUsers);
        } catch (DataIntegrityViolationException ignored) {
            // 동시성으로 동일 멤버가 먼저 삽입된 경우 무시한다.
        }

        return conversation;
    }

    private ChatConversation createGroupConversation(User requester, String title, List<Long> memberIds) {
        LinkedHashSet<Long> ids = memberIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(memberIds);
        ids.add(requester.getId());
        if (ids.size() < 2) {
            throw new IllegalArgumentException("그룹 대화에는 초대할 사용자를 최소 1명 이상 선택해야 합니다.");
        }

        List<User> members = userRepository.findAllById(ids);
        if (members.size() != ids.size()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 ID가 포함되어 있습니다.");
        }

        ChatConversation conversation = conversationRepository.save(ChatConversation.builder()
                .type(ConversationType.GROUP)
                .title(title)
                .createdBy(requester)
                .build());

        // 그룹 멤버 일괄 저장
        saveMembers(conversation, members, requester.getId());
        return conversation;
    }

    private void saveMembers(ChatConversation conversation, List<User> members) {
        saveMembers(conversation, members, null);
    }

    private void saveMembers(ChatConversation conversation, List<User> members, Long ownerId) {
        List<ChatConversationMember> entities = members.stream()
                .map(user -> ChatConversationMember.builder()
                        .id(new ChatConversationMemberId(conversation.getId(), user.getId()))
                        .conversation(conversation)
                        .user(user)
                        .role(ownerId != null && ownerId.equals(user.getId())
                                ? ChatParticipantRole.OWNER
                                : ChatParticipantRole.MEMBER)
                        .build())
                .collect(Collectors.toList());
        conversationMemberRepository.saveAll(entities);
    }

    private void cleanupConversation(Long conversationId, ChatConversation conversation) {
        // self-FK(reply_to_message_id) 제약으로 인한 삭제 충돌을 막기 위해 참조를 먼저 해제한다.
        messageRepository.clearReplyReferences(conversationId);
        messageRepository.deleteByConversation_Id(conversationId);
        conversationRepository.delete(conversation);
    }

    private String buildDirectKey(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + ":" + max;
    }

    private void ensureMembership(Long conversationId, Long userId) {
        // 대화방 멤버십 검증 실패 시 403
        if (!conversationMemberRepository.existsActiveByConversationIdAndUserId(conversationId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("대화방 멤버만 접근할 수 있습니다.");
        }
    }

    private ChatConversationMember getActiveMemberOrThrow(Long conversationId, Long userId) {
        return conversationMemberRepository.findActiveByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("대화방 멤버만 접근할 수 있습니다."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ChatDto.MessageResponse toMessageResponse(ChatMessage message, Map<Long, String> avatarUrls) {
        ChatDto.MessageResponse r = new ChatDto.MessageResponse();
        r.setId(message.getId());
        r.setConversationId(message.getConversation().getId());
        r.setSenderId(message.getSender().getId());
        r.setSenderAvatarUrl(avatarUrls.get(message.getSender().getId()));
        r.setClientMsgId(message.getClientMsgId());
        r.setType(message.getType());
        r.setBody(message.getBody());
        r.setMetadata(message.getMetadata());
        r.setReplyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null);
        r.setCreatedAt(message.getCreatedAt());
        return r;
    }

    private ChatDto.ConversationSummaryResponse toSummary(ChatConversation conversation,
                                                          ChatMessage lastMessage,
                                                          Long currentUserId) {
        ChatDto.ConversationSummaryResponse r = new ChatDto.ConversationSummaryResponse();
        r.setConversationId(conversation.getId());
        r.setType(conversation.getType());
        r.setTitle(conversation.getTitle());
        List<ConversationParticipantProjection> participants =
                conversationMemberRepository.findParticipantSummariesByConversationId(conversation.getId());
        Map<Long, String> avatarUrls = userAvatarService.getAvatarUrls(buildConversationAvatarLookupIds(
                participants,
                lastMessage != null && lastMessage.getSender() != null ? lastMessage.getSender().getId() : null));
        r.setDisplayTitle(resolveDisplayTitle(conversation.getType(), conversation.getTitle(), currentUserId, participants));
        r.setAvatarUrl(resolveConversationAvatarUrl(conversation.getType(), currentUserId, participants, avatarUrls));
        r.setDirectKey(conversation.getDirectKey());
        r.setLastMessage(lastMessage != null ? toMessageResponse(lastMessage, avatarUrls) : null);
        r.setLastActivityAt(lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt());
        r.setUnreadMessageCount(0L);
        r.setParticipants(toChatUsers(participants, currentUserId, avatarUrls));
        r.setParticipantCount((long) r.getParticipants().size());
        r.setHidden(false);
        return r;
    }

    private ChatDto.ConversationSummaryResponse toSummaryFromProjection(ConversationSummaryProjection p,
                                                                        Long currentUserId,
                                                                        List<ConversationParticipantProjection> participants,
                                                                        Map<Long, String> avatarUrls) {
        ChatDto.ConversationSummaryResponse r = new ChatDto.ConversationSummaryResponse();
        r.setConversationId(p.getConversationId());
        r.setType(ConversationType.valueOf(p.getConversationType()));
        r.setTitle(p.getTitle());
        r.setDisplayTitle(resolveDisplayTitle(r.getType(), p.getTitle(), currentUserId, participants));
        r.setAvatarUrl(resolveConversationAvatarUrl(r.getType(), currentUserId, participants, avatarUrls));
        r.setDirectKey(p.getDirectKey());
        r.setUnreadMessageCount(p.getUnreadMessageCount() != null ? p.getUnreadMessageCount() : 0L);
        r.setParticipants(toChatUsers(participants, currentUserId, avatarUrls));
        r.setParticipantCount((long) r.getParticipants().size());
        r.setHidden(p.getHiddenAt() != null);

        if (p.getLastMessageId() != null) {
            ChatDto.MessageResponse m = new ChatDto.MessageResponse();
            m.setId(p.getLastMessageId());
            m.setConversationId(p.getConversationId());
            m.setSenderId(p.getLastSenderId());
            m.setSenderAvatarUrl(avatarUrls.get(p.getLastSenderId()));
            m.setBody(p.getLastMessageBody());
            m.setCreatedAt(p.getLastMessageCreatedAt());
            r.setLastMessage(m);
            r.setLastActivityAt(p.getLastMessageCreatedAt());
        } else {
            r.setLastActivityAt(null);
        }
        return r;
    }

    private Map<Long, List<ConversationParticipantProjection>> loadParticipantsForConversations(
            List<ConversationSummaryProjection> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = rows.stream()
                .map(ConversationSummaryProjection::getConversationId)
                .distinct()
                .toList();
        return conversationMemberRepository.findParticipantSummariesByConversationIds(conversationIds).stream()
                .collect(Collectors.groupingBy(
                        ConversationParticipantProjection::getConversationId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private String resolveDisplayTitle(ConversationType type,
                                       String rawTitle,
                                       Long currentUserId,
                                       List<ConversationParticipantProjection> participants) {
        if (type == ConversationType.DIRECT) {
            return resolveDirectDisplayTitle(rawTitle, currentUserId, participants);
        }
        return resolveGroupDisplayTitle(rawTitle, currentUserId, participants);
    }

    private String resolveDirectDisplayTitle(String rawTitle,
                                             Long currentUserId,
                                             List<ConversationParticipantProjection> participants) {
        if (participants != null && !participants.isEmpty()) {
            for (ConversationParticipantProjection participant : participants) {
                String displayName = resolveParticipantDisplayName(participant);
                if (!Objects.equals(participant.getUserId(), currentUserId) && isNotBlank(displayName)) {
                    return displayName;
                }
            }
            for (ConversationParticipantProjection participant : participants) {
                String displayName = resolveParticipantDisplayName(participant);
                if (isNotBlank(displayName)) {
                    return displayName;
                }
            }
        }
        if (isNotBlank(rawTitle)) {
            return rawTitle;
        }
        return "알 수 없는 사용자";
    }

    private String resolveGroupDisplayTitle(String rawTitle,
                                            Long currentUserId,
                                            List<ConversationParticipantProjection> participants) {
        if (isNotBlank(rawTitle)) {
            return rawTitle;
        }

        if (participants == null || participants.isEmpty()) {
            return "이름 없는 단체방";
        }

        List<String> others = participants.stream()
                .filter(participant -> !Objects.equals(participant.getUserId(), currentUserId))
                .map(this::resolveParticipantDisplayName)
                .filter(this::isNotBlank)
                .toList();
        List<String> candidates = others.isEmpty()
                ? participants.stream()
                        .map(this::resolveParticipantDisplayName)
                        .filter(this::isNotBlank)
                        .toList()
                : others;

        if (candidates.isEmpty()) {
            return "이름 없는 단체방";
        }
        if (candidates.size() <= 2) {
            return String.join(", ", candidates);
        }
        return candidates.get(0) + ", " + candidates.get(1) + " ...";
    }

    private Map<Long, String> loadConversationAvatarUrls(List<ConversationSummaryProjection> rows,
                                                         Map<Long, List<ConversationParticipantProjection>> participantsByConversationId) {
        Set<Long> lookupIds = new LinkedHashSet<>();
        rows.forEach(row -> {
            lookupIds.add(row.getLastSenderId());
            List<ConversationParticipantProjection> participants = participantsByConversationId.get(row.getConversationId());
            if (participants != null) {
                participants.stream()
                        .map(ConversationParticipantProjection::getUserId)
                        .forEach(lookupIds::add);
            }
        });
        return userAvatarService.getAvatarUrls(lookupIds);
    }

    private Set<Long> buildConversationAvatarLookupIds(List<ConversationParticipantProjection> participants, Long senderId) {
        Set<Long> lookupIds = new LinkedHashSet<>();
        if (senderId != null) {
            lookupIds.add(senderId);
        }
        if (participants != null) {
            participants.stream()
                    .map(ConversationParticipantProjection::getUserId)
                    .forEach(lookupIds::add);
        }
        return lookupIds;
    }

    private String resolveConversationAvatarUrl(ConversationType type,
                                                Long currentUserId,
                                                List<ConversationParticipantProjection> participants,
                                                Map<Long, String> avatarUrls) {
        if (type != ConversationType.DIRECT || participants == null || participants.isEmpty()) {
            return null;
        }

        for (ConversationParticipantProjection participant : participants) {
            if (!Objects.equals(participant.getUserId(), currentUserId)) {
                return avatarUrls.get(participant.getUserId());
            }
        }

        return avatarUrls.get(participants.get(0).getUserId());
    }

    private List<ChatDto.ChatUserResponse> toChatUsers(List<ConversationParticipantProjection> participants,
                                                       Long currentUserId,
                                                       Map<Long, String> avatarUrls) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        return participants.stream()
                .map(participant -> {
                    ChatDto.ChatUserResponse response = new ChatDto.ChatUserResponse();
                    response.setUserId(participant.getUserId());
                    response.setUsername(participant.getUsername());
                    response.setName(participant.getName());
                    response.setNickname(participant.getNickname());
                    response.setAvatarUrl(avatarUrls.get(participant.getUserId()));
                    response.setMe(Objects.equals(participant.getUserId(), currentUserId));
                    return response;
                })
                .toList();
    }

    private String resolveParticipantDisplayName(ConversationParticipantProjection participant) {
        if (participant == null) {
            return null;
        }
        if (isNotBlank(participant.getNickname())) {
            return participant.getNickname();
        }
        if (isNotBlank(participant.getName())) {
            return participant.getName();
        }
        return participant.getUsername();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void broadcastMessageCreated(Long conversationId, ChatDto.MessageResponse messageResponse) {
        ChatDto.ConversationEvent event = new ChatDto.ConversationEvent();
        event.setType("MESSAGE_CREATED");
        event.setConversationId(conversationId);
        event.setMessage(messageResponse);

        // 구독 채널: /topic/conversations/{conversationId}
        realtimeEventPublisher.publishConversationMessage(conversationId, event);
    }

    private void broadcastUnreadCountForRecipients(Long conversationId, Long senderId, Collection<Long> participants) {
        participants.stream()
                .filter(userId -> !userId.equals(senderId))
                .forEach(userId -> publishUnreadCountForUser(conversationId, userId));
    }

    private void publishUnreadCountForUser(Long conversationId, Long userId) {
        long unreadMessageCount = conversationMemberRepository.countUnreadMessages(conversationId, userId);
        long totalUnreadMessageCount = conversationMemberRepository.countTotalUnreadMessages(userId);

        ChatDto.ConversationUnreadCountEvent event = new ChatDto.ConversationUnreadCountEvent();
        event.setType("CONVERSATION_UNREAD_COUNT_UPDATED");
        event.setConversationId(conversationId);
        event.setUnreadMessageCount(unreadMessageCount);
        event.setTotalUnreadMessageCount(totalUnreadMessageCount);

        realtimeEventPublisher.publishConversationUnreadCount(userId, event);
    }

    private void publishThreadUpdatedForUser(Long conversationId, Long userId) {
        Map<String, Object> payload = Map.of(
                "threadId", conversationId,
                "unreadMessageCount", conversationMemberRepository.countUnreadMessages(conversationId, userId),
                "totalUnreadMessageCount", conversationMemberRepository.countTotalUnreadMessages(userId));
        publishUserEvent(userId, UserEventType.CHAT_THREAD_UPDATED.value(), payload);
    }

    private void publishMessageCreatedToUserEvents(Long conversationId,
                                                   ChatDto.MessageResponse messageResponse,
                                                   Collection<Long> participants) {
        Map<String, Object> payload = Map.of(
                "threadId", conversationId,
                "message", messageResponse);
        for (Long participantId : participants) {
            publishUserEvent(participantId, UserEventType.CHAT_MESSAGE_CREATED.value(), payload);
            publishThreadUpdatedForUser(conversationId, participantId);
        }
    }

    private void publishUserEvent(Long userId, String eventType, Object payload) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        realtimeEventPublisher.publishUserEvent(user.getUsername(), user.getId(), eventType, payload);
    }

    private void runAfterCommitOrNow(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션 동기화가 있으면 커밋 이후 실행
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            // 트랜잭션 밖이면 즉시 실행
            runnable.run();
        }
    }

    public record SendResult(ChatDto.MessageResponse message, boolean deduplicated) {
    }
}
