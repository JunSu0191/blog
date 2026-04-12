package com.study.blog.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.chat.dto.ChatDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createConversationRequestShouldAcceptInviteeIdAlias() throws Exception {
        String json = """
                {
                  "type": "GROUP",
                  "title": "팀 채팅",
                  "inviteeId": 7
                }
                """;

        ChatDto.CreateConversationRequest request =
                objectMapper.readValue(json, ChatDto.CreateConversationRequest.class);

        assertThat(request.getMemberIds()).containsExactly(7L);
    }

    @Test
    void createConversationRequestShouldAcceptParticipantObjects() throws Exception {
        String json = """
                {
                  "type": "GROUP",
                  "title": "팀 채팅",
                  "participants": [
                    { "userId": 2 },
                    { "id": 3 },
                    { "participant_id": 4 }
                  ]
                }
                """;

        ChatDto.CreateConversationRequest request =
                objectMapper.readValue(json, ChatDto.CreateConversationRequest.class);

        assertThat(request.getMemberIds()).containsExactly(2L, 3L, 4L);
    }

    @Test
    void createConversationRequestShouldAcceptParticipantUserIdsAlias() throws Exception {
        String json = """
                {
                  "type": "GROUP",
                  "participantUserIds": [1]
                }
                """;

        ChatDto.CreateConversationRequest request =
                objectMapper.readValue(json, ChatDto.CreateConversationRequest.class);

        assertThat(request.getMemberIds()).containsExactly(1L);
    }
}
