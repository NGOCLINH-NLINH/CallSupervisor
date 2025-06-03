package org.example.agentserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.agentserver.repository.LiveChannelEventRepository;
import org.example.agentserver.websocket.WebSocketMessageSender;
import org.example.commonmodel.model.ChannelEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChannelEventConsumerService {
    private final LiveChannelEventRepository repository;
    private final WebSocketMessageSender webSocketMessageSender;
    private final AgentDashboardService dashboardService;

    public ChannelEventConsumerService(LiveChannelEventRepository liveChannelEventRepository,
                                       WebSocketMessageSender webSocketMessageSender,
                                       AgentDashboardService agentDashboardService) {
        this.repository = liveChannelEventRepository;
        this.webSocketMessageSender = webSocketMessageSender;
        this.dashboardService = agentDashboardService;
    }

    @KafkaListener(topics = "${kafka.topic.input-stream}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, ChannelEvent> record) {
        ChannelEvent event = record.value();
        log.info("Received ChannelEvent from Kafka: Key = {}, State = {}, Queue = {}, Vc# = {}, OwnerId = {}",
                record.key(), event.getState(), event.getQueueId(), event.getVcNumber(), event.getOwnerId());

        repository.save(event);
        webSocketMessageSender.sendLiveCallUpdate(event);
        webSocketMessageSender.sendDashboardStatisticsUpdate(dashboardService.getDashboardStatistics());
    }
}
