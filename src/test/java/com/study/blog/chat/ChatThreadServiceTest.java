package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.dto.ChatDto;
import com.study.blog.chat.social.FriendshipService;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatThreadServiceTest {

    @Mock
    private ChatService chatService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private UserRepository userRepository;

    private ChatThreadService chatThreadService;

    @BeforeEach
    void setUp() {
        chatThreadService = new ChatThreadService(chatService, friendshipService, userRepository);
    }

    @Test
    void listThreadsShouldExposeParticipantFields() {
        ChatDto.ChatUserResponse participant = new ChatDto.ChatUserResponse();
        participant.setUserId(1L);
        participant.setNickname("준수");

        ChatDto.ConversationSummaryResponse summary = new ChatDto.ConversationSummaryResponse();
        summary.setConversationId(10L);
        summary.setType(ConversationType.GROUP);
        summary.setParticipantCount(1L);
        summary.setParticipants(List.of(participant));

        when(chatService.listConversations(1L, ConversationType.GROUP)).thenReturn(List.of(summary));

        List<ChatContractDto.ThreadSummaryResponse> responses =
                chatThreadService.listThreads(1L, ConversationType.GROUP);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getParticipantCount()).isEqualTo(1L);
        assertThat(responses.get(0).getParticipants()).hasSize(1);
        assertThat(responses.get(0).getParticipants().get(0).getNickname()).isEqualTo("준수");
    }

    @Test
    void listThreadParticipantsShouldMapConversationParticipants() {
        ChatDto.ChatUserResponse participant = new ChatDto.ChatUserResponse();
        participant.setUserId(2L);
        participant.setUsername("minji");

        ChatDto.ConversationParticipantsResponse source = new ChatDto.ConversationParticipantsResponse();
        source.setParticipantCount(1L);
        source.setParticipants(List.of(participant));

        when(chatService.listConversationParticipants(1L, 33L)).thenReturn(source);

        ChatContractDto.ThreadParticipantsResponse response =
                chatThreadService.listThreadParticipants(1L, 33L);

        assertThat(response.getParticipantCount()).isEqualTo(1L);
        assertThat(response.getParticipants()).hasSize(1);
        assertThat(response.getParticipants().get(0).getUsername()).isEqualTo("minji");
    }
}
