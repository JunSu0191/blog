package com.study.blog.chat.social;

import com.study.blog.chat.*;
import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.realtime.UserEventType;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GroupInviteService {

    private final GroupInviteRepository groupInviteRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final long defaultExpiresInSeconds;

    public GroupInviteService(GroupInviteRepository groupInviteRepository,
                              ChatConversationRepository conversationRepository,
                              ChatConversationMemberRepository conversationMemberRepository,
                              UserRepository userRepository,
                              FriendshipService friendshipService,
                              RealtimeEventPublisher realtimeEventPublisher,
                              @Value("${app.chat.group-invite-expire-seconds:86400}") long defaultExpiresInSeconds) {
        this.groupInviteRepository = groupInviteRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.defaultExpiresInSeconds = defaultExpiresInSeconds;
    }

    public ChatContractDto.GroupInviteResponse createInvite(Long inviterId,
                                                            Long groupThreadId,
                                                            Long inviteeId,
                                                            Long expiresInSeconds) {
        if (inviterId.equals(inviteeId)) {
            throw new IllegalArgumentException("본인은 초대할 수 없습니다.");
        }
        ChatConversation groupThread = getGroupThreadOrThrow(groupThreadId);
        User inviter = getUserOrThrow(inviterId);
        User invitee = getUserOrThrow(inviteeId);

        ChatConversationMember inviterMember = conversationMemberRepository
                .findActiveByConversationIdAndUserId(groupThreadId, inviterId)
                .orElseThrow(() -> new AccessDeniedException("그룹 참여자만 초대할 수 있습니다."));
        ensureInviterPermission(groupThread, inviterMember, inviterId);
        friendshipService.ensureNotBlocked(inviterId, inviteeId);

        if (conversationMemberRepository.existsActiveByConversationIdAndUserId(groupThreadId, inviteeId)) {
            throw new IllegalStateException("이미 그룹에 참여 중인 사용자입니다.");
        }

        expirePendingInvites();
        GroupInvite existingPending = groupInviteRepository
                .findTopByGroupThread_IdAndInvitee_IdAndStatusOrderByCreatedAtDesc(
                        groupThreadId,
                        inviteeId,
                        GroupInviteStatus.PENDING)
                .orElse(null);
        if (existingPending != null) {
            return toInviteResponse(existingPending);
        }

        long ttl = expiresInSeconds != null ? expiresInSeconds : defaultExpiresInSeconds;
        LocalDateTime now = LocalDateTime.now();
        GroupInvite created = groupInviteRepository.save(GroupInvite.builder()
                .groupThread(groupThread)
                .inviter(inviter)
                .invitee(invitee)
                .status(GroupInviteStatus.PENDING)
                .expiresAt(now.plusSeconds(Math.max(ttl, 60)))
                .build());

        ChatContractDto.GroupInviteResponse response = toInviteResponse(created);
        publishUserEvent(invitee, UserEventType.GROUP_INVITE_CREATED, Map.of("invite", response));
        publishUserEvent(inviter, UserEventType.GROUP_INVITE_UPDATED, Map.of("invite", response));
        return response;
    }

    public List<ChatContractDto.GroupInviteResponse> listInvites(Long inviteeId, GroupInviteStatus status) {
        GroupInviteStatus targetStatus = status != null ? status : GroupInviteStatus.PENDING;
        if (targetStatus == GroupInviteStatus.PENDING) {
            expirePendingInvites();
        }
        return groupInviteRepository.findByInvitee_IdAndStatusOrderByCreatedAtDesc(inviteeId, targetStatus).stream()
                .map(this::toInviteResponse)
                .toList();
    }

    public ChatContractDto.GroupInviteResponse acceptInvite(Long userId, Long inviteId) {
        expirePendingInvites();
        GroupInvite invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 초대를 찾을 수 없습니다."));
        ensureInvitee(invite, userId);

        if (invite.getStatus() == GroupInviteStatus.ACCEPTED) {
            return toInviteResponse(invite);
        }
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new IllegalStateException("대기 중인 초대만 수락할 수 있습니다.");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(GroupInviteStatus.EXPIRED);
            invite.setRespondedAt(LocalDateTime.now());
            throw new IllegalStateException("초대가 만료되었습니다.");
        }

        ChatConversation groupThread = getGroupThreadOrThrow(invite.getGroupThread().getId());
        friendshipService.ensureNotBlocked(invite.getInviter().getId(), invite.getInvitee().getId());

        ChatConversationMember existing = conversationMemberRepository
                .findByConversation_IdAndUser_Id(groupThread.getId(), userId)
                .orElse(null);
        if (existing == null) {
            ChatConversationMember member = ChatConversationMember.builder()
                    .id(new ChatConversationMemberId(groupThread.getId(), userId))
                    .conversation(groupThread)
                    .user(invite.getInvitee())
                    .role(ChatParticipantRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            conversationMemberRepository.save(member);
        } else {
            existing.setLeftAt(null);
            existing.setHiddenAt(null);
        }

        invite.setStatus(GroupInviteStatus.ACCEPTED);
        invite.setRespondedAt(LocalDateTime.now());

        ChatContractDto.GroupInviteResponse response = toInviteResponse(invite);
        publishUserEvent(invite.getInviter(), UserEventType.GROUP_INVITE_UPDATED, Map.of("invite", response));
        publishUserEvent(invite.getInvitee(), UserEventType.GROUP_INVITE_UPDATED, Map.of("invite", response));
        publishUserEvent(invite.getInvitee(), UserEventType.CHAT_THREAD_UPDATED, Map.of("threadId", groupThread.getId()));
        return response;
    }

    public ChatContractDto.GroupInviteResponse rejectInvite(Long userId, Long inviteId) {
        expirePendingInvites();
        GroupInvite invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 초대를 찾을 수 없습니다."));
        ensureInvitee(invite, userId);

        if (invite.getStatus() == GroupInviteStatus.REJECTED) {
            return toInviteResponse(invite);
        }
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new IllegalStateException("대기 중인 초대만 거절할 수 있습니다.");
        }

        invite.setStatus(GroupInviteStatus.REJECTED);
        invite.setRespondedAt(LocalDateTime.now());

        ChatContractDto.GroupInviteResponse response = toInviteResponse(invite);
        publishUserEvent(invite.getInviter(), UserEventType.GROUP_INVITE_UPDATED, Map.of("invite", response));
        publishUserEvent(invite.getInvitee(), UserEventType.GROUP_INVITE_UPDATED, Map.of("invite", response));
        return response;
    }

    public void expirePendingInvites() {
        groupInviteRepository.markExpiredInvites(
                LocalDateTime.now(),
                GroupInviteStatus.PENDING,
                GroupInviteStatus.EXPIRED);
    }

    private void ensureInviterPermission(ChatConversation groupThread,
                                         ChatConversationMember inviterMember,
                                         Long inviterId) {
        boolean hasRolePermission = inviterMember.getRole() == ChatParticipantRole.OWNER
                || inviterMember.getRole() == ChatParticipantRole.ADMIN;
        if (hasRolePermission) {
            return;
        }
        if (groupThread.getCreatedBy() != null && inviterId.equals(groupThread.getCreatedBy().getId())) {
            return;
        }
        throw new AccessDeniedException("그룹 OWNER/ADMIN만 초대할 수 있습니다.");
    }

    private void ensureInvitee(GroupInvite invite, Long userId) {
        if (!invite.getInvitee().getId().equals(userId)) {
            throw new AccessDeniedException("본인에게 온 초대만 처리할 수 있습니다.");
        }
    }

    private ChatConversation getGroupThreadOrThrow(Long groupThreadId) {
        ChatConversation thread = conversationRepository.findById(groupThreadId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 채팅을 찾을 수 없습니다."));
        if (thread.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("GROUP 채팅만 초대할 수 있습니다.");
        }
        return thread;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ChatContractDto.GroupInviteResponse toInviteResponse(GroupInvite invite) {
        ChatContractDto.GroupInviteResponse response = new ChatContractDto.GroupInviteResponse();
        response.setId(invite.getId());
        response.setGroupThreadId(invite.getGroupThread().getId());
        response.setInviterId(invite.getInviter().getId());
        response.setInviteeId(invite.getInvitee().getId());
        response.setStatus(invite.getStatus());
        response.setCreatedAt(invite.getCreatedAt());
        response.setRespondedAt(invite.getRespondedAt());
        response.setExpiresAt(invite.getExpiresAt());
        return response;
    }

    private void publishUserEvent(User user, UserEventType eventType, Object payload) {
        realtimeEventPublisher.publishUserEvent(user.getUsername(), user.getId(), eventType.value(), payload);
    }
}
