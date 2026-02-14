package com.study.blog.notification;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.notification.dto.NotificationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserResolver currentUserResolver;

    public NotificationController(NotificationService notificationService, CurrentUserResolver currentUserResolver) {
        this.notificationService = notificationService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<List<NotificationDto.Response>>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(notificationService.list(userId, cursorId, size));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponseTemplate<NotificationDto.Response>> read(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long id) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(notificationService.read(userId, id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> readAll(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        int updated = notificationService.readAll(userId);
        return ApiResponseFactory.ok(Map.of("updated", updated));
    }
}
