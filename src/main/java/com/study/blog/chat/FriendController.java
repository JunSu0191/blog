package com.study.blog.chat;

import com.study.blog.chat.dto.ChatContractDto;
import com.study.blog.chat.social.FriendRequestListType;
import com.study.blog.chat.social.FriendshipService;
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
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendshipService friendshipService;
    private final CurrentUserResolver currentUserResolver;

    public FriendController(FriendshipService friendshipService,
                            CurrentUserResolver currentUserResolver) {
        this.friendshipService = friendshipService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<List<ChatContractDto.FriendResponse>>> listFriends(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(friendshipService.listFriends(userId));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponseTemplate<List<ChatContractDto.FriendRequestResponse>>> listRequests(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam(name = "type", defaultValue = "received") String type) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        FriendRequestListType queryType = parseType(type);
        return ApiResponseFactory.ok(friendshipService.listRequests(userId, queryType));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.FriendRequestResponse>> sendRequest(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody ChatContractDto.CreateFriendRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        ChatContractDto.FriendRequestResponse response = friendshipService.sendRequest(userId, request.getTargetUserId());
        return ApiResponseFactory.created(URI.create("/api/friends/requests/" + response.getId()), response);
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.FriendRequestResponse>> accept(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long requestId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(friendshipService.acceptRequest(userId, requestId));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.FriendRequestResponse>> reject(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long requestId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(friendshipService.rejectRequest(userId, requestId));
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.FriendRequestCancelResponse>> cancelRequest(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long requestId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(friendshipService.cancelRequest(userId, requestId));
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<ApiResponseTemplate<Void>> removeFriend(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long friendUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        friendshipService.removeFriend(userId, friendUserId);
        return ApiResponseFactory.noContent();
    }

    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<ApiResponseTemplate<ChatContractDto.FriendRequestResponse>> block(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long targetUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(friendshipService.blockUser(userId, targetUserId));
    }

    private FriendRequestListType parseType(String raw) {
        if (raw == null) {
            return FriendRequestListType.RECEIVED;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RECEIVED" -> FriendRequestListType.RECEIVED;
            case "SENT" -> FriendRequestListType.SENT;
            default -> throw new IllegalArgumentException("type은 received 또는 sent만 허용됩니다.");
        };
    }
}
