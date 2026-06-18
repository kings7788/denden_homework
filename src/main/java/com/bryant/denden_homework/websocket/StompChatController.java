package com.bryant.denden_homework.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/** STOMP chat: receives messages on /app/chat and broadcasts to /topic/public. */
@Slf4j
@Controller
public class StompChatController {

    /**
     * Client SENDs to "/app/chat"; the return value is published by the broker to
     * "/topic/public", reaching everyone SUBSCRIBED there.
     */
    @MessageMapping("/chat")
    @SendTo("/topic/public")
    public ChatMessage broadcast(ChatMessage message) {
        log.info("STOMP chat from {}: {}", message.getSender(), message.getContent());
        return message;
    }
}
