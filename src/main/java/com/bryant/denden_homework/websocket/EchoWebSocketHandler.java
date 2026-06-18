package com.bryant.denden_homework.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Echo handler: whatever a client sends, we send the same text straight back.
 * Extends TextWebSocketHandler so we only deal with text messages.
 */
@Slf4j
@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

    /** Called once, right after the WebSocket connection is opened. */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WS connected: {}", session.getId());
    }

    /** Called every time the client sends a text message. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String received = message.getPayload();
        log.info("WS received from {}: {}", session.getId(), received);

        // Echo: send the same message straight back to this connection.
        session.sendMessage(message);
    }

    /** Called once when the connection closes. */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WS closed: {} ({})", session.getId(), status);
    }
}
