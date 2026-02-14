package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.notification.NotificationService;
import com.study.blog.realtime.RealtimeEventPublisher;
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
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final NotificationService notificationService;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatConversationMemberRepository conversationMemberRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       RealtimeEventPublisher realtimeEventPublisher,
                       NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.notificationService = notificationService;
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
            // DIRECT는 항상 "작은ID:큰ID"로 키를 고정해야 중복 방 생성이 방지된다.
            String directKey = buildDirectKey(requesterId, otherId);
            conversation = conversationRepository.findByDirectKey(directKey)
                    .orElseGet(() -> createDirectConversation(requester, directKey, otherId));
        } else {
            conversation = createGroupConversation(requester, req.getTitle(), req.getMemberIds());
        }

        return toSummary(conversation, null);
    }

    public ChatDto.ConversationSummaryResponse createDirectConversation(Long requesterId, Long otherUserId) {
        ChatDto.CreateConversationRequest req = new ChatDto.CreateConversationRequest();
        req.setType(ConversationType.DIRECT);
        req.setOtherUserId(otherUserId);
        return createConversation(requesterId, req);
    }

    @Transactional(readOnly = true)
    public List<ChatDto.ConversationSummaryResponse> listConversations(Long userId) {
        List<ConversationSummaryProjection> rows = conversationRepository.findConversationSummariesByUserId(userId);
        return rows.stream().map(this::toSummaryFromProjection).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.ChatUserResponse> listChatUsers(Long userId) {
        return userRepository.findByDeletedYnOrderByIdAsc("N").stream()
                .map(user -> {
                    ChatDto.ChatUserResponse r = new ChatDto.ChatUserResponse();
                    r.setUserId(user.getId());
                    r.setUsername(user.getUsername());
                    r.setName(user.getName());
                    r.setMe(user.getId().equals(userId));
                    return r;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MessageResponse> getMessages(Long userId, Long conversationId, Long cursorMessageId, int size) {
        // 보안: 대화방 멤버가 아니면 메시지 조회 불가
        ensureMembership(conversationId, userId);
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

        return messageRepository.findPageByConversationIdWithCursor(
                        conversationId,
                        cursorCreatedAt,
                        cursorId,
                        PageRequest.of(0, normalizedSize))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public void markAsRead(Long userId, Long conversationId, Long lastReadMessageId) {
        // 보안: 대화방 멤버만 읽음 처리 가능
        ensureMembership(conversationId, userId);

        ChatMessage lastRead = messageRepository.findByIdAndConversation_Id(lastReadMessageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("lastReadMessageId가 유효하지 않습니다."));

        ChatConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("대화방 멤버를 찾을 수 없습니다."));

        // conversation_member에 마지막 읽은 메시지 정보를 저장한다.
        member.setLastReadMessageId(lastRead.getId());
        member.setLastReadAt(LocalDateTime.now());
    }

    public SendResult sendMessage(Long senderId, Long conversationId, ChatDto.SendMessageRequest req) {
        ensureMembership(conversationId, senderId);

        // 멱등성: 동일 (conversationId, senderId, clientMsgId) 요청은 기존 메시지를 반환
        Optional<ChatMessage> existing = messageRepository.findByConversation_IdAndSender_IdAndClientMsgId(
                conversationId, senderId, req.getClientMsgId());
        if (existing.isPresent()) {
            return new SendResult(toMessageResponse(existing.get()), true);
        }

        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
        User sender = getUserOrThrow(senderId);

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
        ChatDto.MessageResponse messageResponse = toMessageResponse(saved);

        // DB 트랜잭션이 성공적으로 커밋된 뒤에만 실시간/알림을 전송한다.
        // (커밋 전에 보내면 롤백 시 "유령 이벤트"가 생길 수 있음)
        runAfterCommitOrNow(() -> {
            broadcastMessageCreated(conversationId, messageResponse);
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
        return conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId);
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

    private ChatConversation createGroupConversation(User requester, String title, List<Long> memberIds) {
        // memberIds가 비어있으면 MVP 요구사항대로 "전체 활성 사용자"를 그룹 멤버로 사용
        LinkedHashSet<Long> ids = (memberIds == null || memberIds.isEmpty())
                ? userRepository.findByDeletedYnOrderByIdAsc("N").stream()
                        .map(User::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>(memberIds);
        ids.add(requester.getId());
        if (ids.size() < 2) {
            throw new IllegalArgumentException("GROUP 대화는 최소 2명 이상이어야 합니다.");
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
        saveMembers(conversation, members);
        return conversation;
    }

    private void saveMembers(ChatConversation conversation, List<User> members) {
        List<ChatConversationMember> entities = members.stream()
                .map(user -> ChatConversationMember.builder()
                        .id(new ChatConversationMemberId(conversation.getId(), user.getId()))
                        .conversation(conversation)
                        .user(user)
                        .role("MEMBER")
                        .build())
                .collect(Collectors.toList());
        conversationMemberRepository.saveAll(entities);
    }

    private String buildDirectKey(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + ":" + max;
    }

    private void ensureMembership(Long conversationId, Long userId) {
        // 대화방 멤버십 검증 실패 시 403
        if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("대화방 멤버만 접근할 수 있습니다.");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ChatDto.MessageResponse toMessageResponse(ChatMessage message) {
        ChatDto.MessageResponse r = new ChatDto.MessageResponse();
        r.setId(message.getId());
        r.setConversationId(message.getConversation().getId());
        r.setSenderId(message.getSender().getId());
        r.setClientMsgId(message.getClientMsgId());
        r.setType(message.getType());
        r.setBody(message.getBody());
        r.setMetadata(message.getMetadata());
        r.setReplyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null);
        r.setCreatedAt(message.getCreatedAt());
        return r;
    }

    private ChatDto.ConversationSummaryResponse toSummary(ChatConversation conversation, ChatMessage lastMessage) {
        ChatDto.ConversationSummaryResponse r = new ChatDto.ConversationSummaryResponse();
        r.setConversationId(conversation.getId());
        r.setType(conversation.getType());
        r.setTitle(conversation.getTitle());
        r.setDirectKey(conversation.getDirectKey());
        r.setLastMessage(lastMessage != null ? toMessageResponse(lastMessage) : null);
        r.setLastActivityAt(lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt());
        return r;
    }

    private ChatDto.ConversationSummaryResponse toSummaryFromProjection(ConversationSummaryProjection p) {
        ChatDto.ConversationSummaryResponse r = new ChatDto.ConversationSummaryResponse();
        r.setConversationId(p.getConversationId());
        r.setType(ConversationType.valueOf(p.getConversationType()));
        r.setTitle(p.getTitle());
        r.setDirectKey(p.getDirectKey());

        if (p.getLastMessageId() != null) {
            ChatDto.MessageResponse m = new ChatDto.MessageResponse();
            m.setId(p.getLastMessageId());
            m.setConversationId(p.getConversationId());
            m.setSenderId(p.getLastSenderId());
            m.setBody(p.getLastMessageBody());
            m.setCreatedAt(p.getLastMessageCreatedAt());
            r.setLastMessage(m);
            r.setLastActivityAt(p.getLastMessageCreatedAt());
        } else {
            r.setLastActivityAt(null);
        }
        return r;
    }

    private void broadcastMessageCreated(Long conversationId, ChatDto.MessageResponse messageResponse) {
        ChatDto.ConversationEvent event = new ChatDto.ConversationEvent();
        event.setType("MESSAGE_CREATED");
        event.setConversationId(conversationId);
        event.setMessage(messageResponse);

        // 구독 채널: /topic/conversations/{conversationId}
        realtimeEventPublisher.publishConversationMessage(conversationId, event);
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
