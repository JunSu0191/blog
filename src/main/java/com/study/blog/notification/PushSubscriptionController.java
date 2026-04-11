package com.study.blog.notification;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.notification.dto.PushSubscriptionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications/push-subscriptions")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    private final CurrentUserResolver currentUserResolver;

    public PushSubscriptionController(PushSubscriptionService pushSubscriptionService,
                                      CurrentUserResolver currentUserResolver) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponseTemplate<PushSubscriptionDto.Response>> save(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PushSubscriptionDto.SaveRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        PushSubscriptionService.SaveResult result = pushSubscriptionService.save(userId, request);
        if (result.created()) {
            return ApiResponseFactory.created(
                    URI.create("/api/notifications/push-subscriptions/" + result.response().getId()),
                    result.response());
        }
        return ApiResponseFactory.ok(result.response());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponseTemplate<Void>> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PushSubscriptionDto.DeleteRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        pushSubscriptionService.delete(userId, request.getEndpoint());
        return ApiResponseFactory.noContent();
    }
}
