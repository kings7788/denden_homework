package com.bryant.denden_homework.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers WebSocket handlers and the URLs they listen on.
 */
@Configuration
@EnableWebSocket                 // turns on raw WebSocket support
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final EchoWebSocketHandler echoWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Clients connect to ws://<host>/ws/echo and talk to EchoWebSocketHandler.
        registry.addHandler(echoWebSocketHandler, "/ws/echo")
                .setAllowedOriginPatterns("*"); // allow the test page to connect (handshake CORS)

        // Single-room chat: every message is broadcast to all clients on /ws/chat.
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*");
    }
}
