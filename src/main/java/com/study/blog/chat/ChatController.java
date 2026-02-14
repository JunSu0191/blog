package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
/**
 * 채팅 REST API 진입점.
 *
 * 컨트롤러는 "요청 파싱 + 현재 사용자 식별 + 서비스 호출"만 담당하고,
 * 실제 비즈니스 규칙(멤버십/멱등/읽음 처리)은 ChatService가 담당한다.
 */
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserResolver currentUserResolver;

    public ChatController(ChatService chatService, CurrentUserResolver currentUserResolver) {
        this.chatService = chatService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponseTemplate<ChatDto.ConversationSummaryResponse>> createConversation(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody ChatDto.CreateConversationRequest req) {
        // JWT 인증이 있으면 그 사용자, 없으면 개발 fallback 로직으로 사용자 결정
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        ChatDto.ConversationSummaryResponse resp = chatService.createConversation(userId, req);
        return ApiResponseFactory.created(URI.create("/api/chat/conversations/" + resp.getConversationId()), resp);
    }

    @PostMapping("/conversations/direct/{otherUserId}")
    public ResponseEntity<ApiResponseTemplate<ChatDto.ConversationSummaryResponse>> createDirectConversation(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long otherUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        ChatDto.ConversationSummaryResponse resp = chatService.createDirectConversation(userId, otherUserId);
        return ApiResponseFactory.created(URI.create("/api/chat/conversations/" + resp.getConversationId()), resp);
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponseTemplate<List<ChatDto.ChatUserResponse>>> listChatUsers(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(chatService.listChatUsers(userId));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponseTemplate<List<ChatDto.ConversationSummaryResponse>>> listConversations(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(chatService.listConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponseTemplate<List<ChatDto.MessageResponse>>> listMessages(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long id,
            @RequestParam(required = false) Long cursorMessageId,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(chatService.getMessages(userId, id, cursorMessageId, size));
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponseTemplate<Void>> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long id,
            @Valid @RequestBody ChatDto.ReadRequest req) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatService.markAsRead(userId, id, req.getLastReadMessageId());
        return ApiResponseFactory.noContent();
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponseTemplate<Void>> leaveConversation(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long id) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatService.leaveConversation(userId, id);
        return ApiResponseFactory.noContent();
    }
}
