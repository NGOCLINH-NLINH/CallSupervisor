package org.example.agentserver.websocket;

import lombok.extern.slf4j.Slf4j;
import org.example.agentserver.repository.LiveChannelEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {
    private final WebSocketMessageSender messageSender;
    private final LiveChannelEventRepository repository;

    public WebSocketEventListener(WebSocketMessageSender messageSender, LiveChannelEventRepository repository) {
        this.messageSender = messageSender;
        this.repository = repository;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        log.info("WebSocket connected to user " + userId);
        messageSender.sendInitialCallsToUser(userId, repository.findAllActiveCalls());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        log.info("WebSocket disconnected from user: " + userId);
    }
}
