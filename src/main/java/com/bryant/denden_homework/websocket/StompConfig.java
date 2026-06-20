package com.bryant.denden_homework.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup.
 * Coexists with the raw WebSocket handlers (echo/chat) — they use different endpoints.
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * The URL clients open the STOMP connection on. Native WebSocket (no SockJS):
     * the WS handshake uses an Origin check (allowed below), avoiding SockJS's
     * cross-origin XHR /info request and its brittle CORS/credentials handling.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // "/topic": destinations the built-in broker broadcasts to (clients SUBSCRIBE here).
        registry.enableSimpleBroker("/topic");
        // "/app": messages with this prefix are routed to @MessageMapping methods.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
