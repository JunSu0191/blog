package com.study.blog.notification;

import com.study.blog.notification.dto.PushSubscriptionDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock
    private UserRepository userRepository;

    private PushSubscriptionService pushSubscriptionService;

    @BeforeEach
    void setUp() {
        pushSubscriptionService = new PushSubscriptionService(pushSubscriptionRepository, userRepository);
    }

    @Test
    void saveShouldCreateNewSubscription() {
        User user = User.builder().id(1L).username("u1").name("U1").nickname("nick").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pushSubscriptionRepository.findByEndpoint("https://push.example/sub")).thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any(PushSubscription.class))).thenAnswer(invocation -> {
            PushSubscription subscription = invocation.getArgument(0);
            subscription.setId(10L);
            return subscription;
        });

        PushSubscriptionDto.SaveRequest request = request("https://push.example/sub", "key", "auth");

        PushSubscriptionService.SaveResult result = pushSubscriptionService.save(1L, request);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getId()).isEqualTo(10L);
        assertThat(result.response().getEndpoint()).isEqualTo("https://push.example/sub");
    }

    @Test
    void saveShouldReactivateExistingSubscription() {
        User user = User.builder().id(1L).username("u1").name("U1").nickname("nick").build();
        PushSubscription existing = PushSubscription.builder()
                .id(10L)
                .endpoint("https://push.example/sub")
                .p256dh("old")
                .auth("old-auth")
                .active(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pushSubscriptionRepository.findByEndpoint("https://push.example/sub")).thenReturn(Optional.of(existing));
        when(pushSubscriptionRepository.save(existing)).thenReturn(existing);

        PushSubscriptionService.SaveResult result = pushSubscriptionService.save(1L, request("https://push.example/sub", "new", "new-auth"));

        assertThat(result.created()).isFalse();
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getP256dh()).isEqualTo("new");
        assertThat(existing.getAuth()).isEqualTo("new-auth");
    }

    @Test
    void deleteShouldBeIdempotent() {
        when(pushSubscriptionRepository.findByUser_IdAndEndpoint(1L, "https://push.example/sub"))
                .thenReturn(Optional.empty());

        pushSubscriptionService.delete(1L, "https://push.example/sub");

        verify(pushSubscriptionRepository, never()).delete(any());
    }

    private PushSubscriptionDto.SaveRequest request(String endpoint, String p256dh, String auth) {
        PushSubscriptionDto.Keys keys = new PushSubscriptionDto.Keys();
        keys.setP256dh(p256dh);
        keys.setAuth(auth);

        PushSubscriptionDto.SaveRequest request = new PushSubscriptionDto.SaveRequest();
        request.setEndpoint(endpoint);
        request.setKeys(keys);
        request.setUserAgent("Mozilla/5.0");
        return request;
    }
}
