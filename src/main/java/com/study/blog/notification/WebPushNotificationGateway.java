package com.study.blog.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@Primary
public class WebPushNotificationGateway implements PushNotificationGateway {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final ObjectMapper objectMapper;
    private final WebPushProperties webPushProperties;

    public WebPushNotificationGateway(ObjectMapper objectMapper,
                                      WebPushProperties webPushProperties) {
        this.objectMapper = objectMapper;
        this.webPushProperties = webPushProperties;
    }

    @Override
    public PushSendResult send(PushSubscription subscription, WebPushPayload payload) {
        if (!webPushProperties.isConfigured()) {
            return PushSendResult.retryable();
        }

        try {
            PushService pushService = new PushService()
                    .setSubject(webPushProperties.getSubject())
                    .setPublicKey(Utils.loadPublicKey(webPushProperties.getPublicKey()))
                    .setPrivateKey(Utils.loadPrivateKey(webPushProperties.getPrivateKey()));

            nl.martijndwars.webpush.Notification notification = new nl.martijndwars.webpush.Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    objectMapper.writeValueAsBytes(payload)
            );
            pushService.send(notification);
            return PushSendResult.sent();
        } catch (IOException ex) {
            if (isInvalidSubscription(ex)) {
                log.warn("Web Push subscription invalidated: endpoint={}", subscription.getEndpoint(), ex);
                return PushSendResult.invalidated();
            }
            log.warn("Web Push send failed with I/O error: endpoint={}", subscription.getEndpoint(), ex);
            return PushSendResult.retryable();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Web Push send interrupted: endpoint={}", subscription.getEndpoint(), ex);
            return PushSendResult.retryable();
        } catch (ExecutionException ex) {
            if (isInvalidSubscription(ex)) {
                log.warn("Web Push subscription invalidated: endpoint={}", subscription.getEndpoint(), ex);
                return PushSendResult.invalidated();
            }
            log.warn("Web Push send failed with execution error: endpoint={}", subscription.getEndpoint(), ex);
            return PushSendResult.retryable();
        } catch (GeneralSecurityException | JoseException ex) {
            log.warn("Web Push send failed with security error: endpoint={}", subscription.getEndpoint(), ex);
            return PushSendResult.retryable();
        }
    }

    private boolean isInvalidSubscription(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("404") || message.contains("410")
                || message.contains("401") || message.contains("403");
    }
}
