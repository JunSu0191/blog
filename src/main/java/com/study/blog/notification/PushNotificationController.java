package com.study.blog.notification;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.notification.dto.PushSubscriptionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/push")
public class PushNotificationController {

    private final WebPushProperties webPushProperties;

    public PushNotificationController(WebPushProperties webPushProperties) {
        this.webPushProperties = webPushProperties;
    }

    @GetMapping("/public-key")
    public ResponseEntity<ApiResponseTemplate<PushSubscriptionDto.PublicKeyResponse>> getPublicKey() {
        PushSubscriptionDto.PublicKeyResponse response = new PushSubscriptionDto.PublicKeyResponse();
        response.setPublicKey(webPushProperties.getPublicKey());
        return ApiResponseFactory.ok(response);
    }
}
