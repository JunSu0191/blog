package com.study.blog.config;

import com.study.blog.chat.ChatStompSecurityInterceptor;
import com.study.blog.chat.StompConnectAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
/**
 * STOMP/WebSocket 전역 설정.
 *
 * - /ws: 순수 WebSocket 엔드포인트
 * - /ws-sockjs: SockJS fallback 엔드포인트
 * - /app: 클라이언트 SEND prefix
 * - /topic, /queue: 브로커 구독 prefix
 */
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompConnectAuthInterceptor stompConnectAuthInterceptor;
    private final ChatStompSecurityInterceptor chatStompSecurityInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(StompConnectAuthInterceptor stompConnectAuthInterceptor,
                           ChatStompSecurityInterceptor chatStompSecurityInterceptor,
                           AllowedOriginsProvider allowedOriginsProvider) {
        this.stompConnectAuthInterceptor = stompConnectAuthInterceptor;
        this.chatStompSecurityInterceptor = chatStompSecurityInterceptor;
        this.allowedOrigins = allowedOriginsProvider.asArray();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 브라우저가 순수 WebSocket으로 붙을 때 사용
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);

        // 네트워크/프록시 환경에서 WebSocket이 제한될 때 SockJS fallback 사용
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // CONNECT 단계에서 Principal을 먼저 확정한 뒤, SEND/SUBSCRIBE 멤버십 검사 수행
        registration.interceptors(stompConnectAuthInterceptor, chatStompSecurityInterceptor);
    }
}
