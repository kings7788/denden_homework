package com.bryant.denden_homework.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * A single-room chat. Unlike the echo handler (which replies only to the sender),
 * this keeps a list of ALL open connections and broadcasts every message to them.
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * Every currently-connected client. Thread-safe because connections open/close
     * and messages arrive on DIFFERENT threads — a plain HashSet would corrupt.
     */
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session); // remember this connection
        log.info("Chat join: {} (online={})", session.getId(), sessions.size());
        broadcast("[系統] " + nick(session) + " 加入了。目前在線 " + sessions.size() + " 人");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Chat msg from {}: {}", session.getId(), message.getPayload());
        broadcast(nick(session) + ": " + message.getPayload()); // send to everyone
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session); // forget this connection
        log.info("Chat leave: {} (online={})", session.getId(), sessions.size());
        broadcast("[系統] " + nick(session) + " 離開了。目前在線 " + sessions.size() + " 人");
    }

    /** Send one text message to every open connection. */
    private void broadcast(String text) {
        TextMessage out = new TextMessage(text);
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    // A single session must not be written by two threads at once.
                    synchronized (s) {
                        s.sendMessage(out);
                    }
                }
            } catch (IOException e) {
                log.warn("Send to {} failed: {}", s.getId(), e.getMessage());
            }
        }
    }

    /** A short, friendly name derived from the session id. */
    private String nick(WebSocketSession session) {
        return "使用者-" + session.getId().substring(0, 4);
    }
}
