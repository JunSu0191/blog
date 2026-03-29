package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.social.*;
import com.study.blog.realtime.RealtimeEventPublisher;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupInviteServiceTest {

    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private ChatConversationRepository conversationRepository;
    @Mock
    private ChatConversationMemberRepository conversationMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    private GroupInviteService groupInviteService;

    @BeforeEach
    void setUp() {
        groupInviteService = new GroupInviteService(
                groupInviteRepository,
                conversationRepository,
                conversationMemberRepository,
                userRepository,
                friendshipService,
                realtimeEventPublisher,
                86400L);
    }

    @Test
    void createInviteShouldCreatePendingInvite() {
        User inviter = User.builder().id(1L).username("u1").build();
        User invitee = User.builder().id(2L).username("u2").build();
        ChatConversation group = ChatConversation.builder()
                .id(10L)
                .type(ConversationType.GROUP)
                .createdBy(inviter)
                .build();

        ChatConversationMember inviterMember = ChatConversationMember.builder()
                .id(new ChatConversationMemberId(10L, 1L))
                .conversation(group)
                .user(inviter)
                .role(ChatParticipantRole.OWNER)
                .build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(inviter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
        when(conversationMemberRepository.findActiveByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(inviterMember));
        when(conversationMemberRepository.existsActiveByConversationIdAndUserId(10L, 2L)).thenReturn(false);
        when(groupInviteRepository.findTopByGroupThread_IdAndInvitee_IdAndStatusOrderByCreatedAtDesc(
                10L, 2L, GroupInviteStatus.PENDING))
                .thenReturn(Optional.empty());

        GroupInvite saved = GroupInvite.builder()
                .id(50L)
                .groupThread(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(GroupInviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(groupInviteRepository.save(any(GroupInvite.class))).thenReturn(saved);

        ChatContractDto.GroupInviteResponse response = groupInviteService.createInvite(1L, 10L, 2L, 3600L);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo(GroupInviteStatus.PENDING);
        verify(realtimeEventPublisher).publishUserEvent(eq("u2"), eq(2L), eq("group.invite.created"), any());
    }

    @Test
    void acceptInviteShouldJoinGroupAndUpdateStatus() {
        User inviter = User.builder().id(1L).username("u1").build();
        User invitee = User.builder().id(2L).username("u2").build();
        ChatConversation group = ChatConversation.builder()
                .id(20L)
                .type(ConversationType.GROUP)
                .createdBy(inviter)
                .build();

        GroupInvite invite = GroupInvite.builder()
                .id(70L)
                .groupThread(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(GroupInviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(groupInviteRepository.findById(70L)).thenReturn(Optional.of(invite));
        when(conversationRepository.findById(20L)).thenReturn(Optional.of(group));
        when(conversationMemberRepository.findByConversation_IdAndUser_Id(20L, 2L)).thenReturn(Optional.empty());

        ChatContractDto.GroupInviteResponse response = groupInviteService.acceptInvite(2L, 70L);

        assertThat(response.getStatus()).isEqualTo(GroupInviteStatus.ACCEPTED);
        verify(conversationMemberRepository).save(any(ChatConversationMember.class));
        verify(realtimeEventPublisher, atLeastOnce()).publishUserEvent(any(), any(), eq("group.invite.updated"), any());
    }

    @Test
    void rejectInviteShouldUpdateStatus() {
        User inviter = User.builder().id(1L).username("u1").build();
        User invitee = User.builder().id(2L).username("u2").build();
        ChatConversation group = ChatConversation.builder()
                .id(21L)
                .type(ConversationType.GROUP)
                .createdBy(inviter)
                .build();

        GroupInvite invite = GroupInvite.builder()
                .id(80L)
                .groupThread(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(GroupInviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(groupInviteRepository.findById(80L)).thenReturn(Optional.of(invite));

        ChatContractDto.GroupInviteResponse response = groupInviteService.rejectInvite(2L, 80L);

        assertThat(response.getStatus()).isEqualTo(GroupInviteStatus.REJECTED);
    }

    @Test
    void acceptInviteShouldFailWhenExpired() {
        User inviter = User.builder().id(1L).username("u1").build();
        User invitee = User.builder().id(2L).username("u2").build();
        ChatConversation group = ChatConversation.builder().id(22L).type(ConversationType.GROUP).build();

        GroupInvite invite = GroupInvite.builder()
                .id(90L)
                .groupThread(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(GroupInviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();

        when(groupInviteRepository.findById(90L)).thenReturn(Optional.of(invite));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> groupInviteService.acceptInvite(2L, 90L));

        assertThat(ex.getMessage()).contains("만료");
        assertThat(invite.getStatus()).isEqualTo(GroupInviteStatus.EXPIRED);
    }
}
