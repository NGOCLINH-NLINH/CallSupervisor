package org.example.agentserver.websocket;

import lombok.extern.slf4j.Slf4j;
import org.example.commonmodel.model.ChannelEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@Slf4j
public class WebSocketMessageSender {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendLiveCallUpdate(ChannelEvent event) {
        log.debug("Sending live call update via WebSocket: CallId={}, State={}", event.getCallId(), event.getState());
    }

    public void sendInitialCallsToUser(String userId, Object events) {
        log.info("Sending initial live calls to user {}. Count: {}", userId, (events instanceof Collection) ? ((Collection<?>)events).size() : 1);
        messagingTemplate.convertAndSendToUser(userId, "/queue/initial-calls", events);
    }

    public void sendDashboardStatisticsUpdate(Map<String, Object> stats) {
        log.debug("Sending dashboard statistics update via WebSocket");
        messagingTemplate.convertAndSend("/topic/dashboard-stats", stats);
    }
}
