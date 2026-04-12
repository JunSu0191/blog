package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.social.GroupInviteService;
import com.study.blog.chat.social.GroupInviteStatus;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/chats")
public class ChatThreadController {

    private final ChatThreadService chatThreadService;
    private final GroupInviteService groupInviteService;
    private final CurrentUserResolver currentUserResolver;

    public ChatThreadController(ChatThreadService chatThreadService,
                                GroupInviteService groupInviteService,
                                CurrentUserResolver currentUserResolver) {
        this.chatThreadService = chatThreadService;
        this.groupInviteService = groupInviteService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/threads")
    public ResponseEntity<ApiResponseTemplate<List<ChatContractDto.ThreadSummaryResponse>>> listThreads(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam(name = "type", required = false) String type) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(chatThreadService.listThreads(userId, parseConversationType(type)));
    }

    @GetMapping("/threads/{threadId}/members")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.ThreadParticipantsResponse>> listThreadParticipants(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long threadId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(chatThreadService.listThreadParticipants(userId, threadId));
    }

    @PatchMapping("/threads/{threadId}/hide")
    public ResponseEntity<ApiResponseTemplate<Void>> hideThread(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long threadId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatThreadService.hideThread(userId, threadId);
        return ApiResponseFactory.noContent();
    }

    @PatchMapping("/threads/{threadId}/unhide")
    public ResponseEntity<ApiResponseTemplate<Void>> unhideThread(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long threadId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatThreadService.unhideThread(userId, threadId);
        return ApiResponseFactory.noContent();
    }

    @DeleteMapping("/threads/{threadId}/messages/me")
    public ResponseEntity<ApiResponseTemplate<Void>> clearMyMessages(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long threadId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatThreadService.clearMyMessages(userId, threadId);
        return ApiResponseFactory.noContent();
    }

    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<ApiResponseTemplate<Void>> leaveGroup(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long groupId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        chatThreadService.leaveGroup(userId, groupId);
        return ApiResponseFactory.noContent();
    }

    @PostMapping("/direct/start")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.ThreadSummaryResponse>> startDirect(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody ChatContractDto.StartDirectRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        ChatContractDto.ThreadSummaryResponse response =
                chatThreadService.startDirect(userId, request.getTargetUserId());
        return ApiResponseFactory.created(URI.create("/api/chats/threads/" + response.getThreadId()), response);
    }

    @PostMapping("/groups/{groupId}/invites")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.GroupInviteResponse>> createGroupInvite(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable("groupId") Long groupId,
            @Valid @RequestBody ChatContractDto.CreateGroupInviteRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        ChatContractDto.GroupInviteResponse response = groupInviteService.createInvite(
                userId,
                groupId,
                request.getInviteeId(),
                request.getExpiresInSeconds());
        return ApiResponseFactory.created(URI.create("/api/chats/invites/" + response.getId()), response);
    }

    @GetMapping("/invites")
    public ResponseEntity<ApiResponseTemplate<List<ChatContractDto.GroupInviteResponse>>> listInvites(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam(name = "status", defaultValue = "PENDING") String status) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        GroupInviteStatus parsedStatus = parseInviteStatus(status);
        return ApiResponseFactory.ok(groupInviteService.listInvites(userId, parsedStatus));
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.GroupInviteResponse>> acceptInvite(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long inviteId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(groupInviteService.acceptInvite(userId, inviteId));
    }

    @PostMapping("/invites/{inviteId}/reject")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.GroupInviteResponse>> rejectInvite(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long inviteId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(groupInviteService.rejectInvite(userId, inviteId));
    }

    private ConversationType parseConversationType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DIRECT" -> ConversationType.DIRECT;
            case "GROUP" -> ConversationType.GROUP;
            default -> throw new IllegalArgumentException("type은 DIRECT 또는 GROUP만 허용됩니다.");
        };
    }

    private GroupInviteStatus parseInviteStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return GroupInviteStatus.PENDING;
        }
        try {
            return GroupInviteStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status 값이 유효하지 않습니다.");
        }
    }
}
