package org.example.calleventproducerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.calleventproducerservice.model.MockAgent;
import org.example.calleventproducerservice.model.MockAgentManager;
import org.springframework.beans.factory.annotation.Value;
//import org.example.calleventproducerservice.model.CallEvent;
import org.example.commonmodel.model.CallEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CallEventProducerService {
    private static final Logger log = LoggerFactory.getLogger(CallEventProducerService.class);

    private final KafkaTemplate<String, CallEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final MockAgentManager agentManager;

    @Value("${kafka.topic.output-stream}")
    private String outputTopic;

    //Sample data
    public static final List<String> QUEUE_IDS = Arrays.asList("da92390c8c96b7cdebc9f5a6d8963710", "queue-sales-01", "queue-info-support-02", "queue-tech-support-03", "queue-complain-04");
    public static final List<String> AGENT_IDS = Arrays.asList("5150aa4a80325ff50e238d388bef79ec", "agent-001", "agent-002", "agent-003", "agent-004", "agent-005", "agent-006", "agent-007", "agent-008", "agent-009", "agent-010");
    public static final List<String> VC_NUMBERS = Arrays.asList("1068", "19001000", "19002000", "18001000", "18002000");

    public static final Random random = new Random();

    public CallEventProducerService(KafkaTemplate<String, CallEvent> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    ResourceLoader resourceLoader,
                                    MockAgentManager agentManager) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.agentManager = agentManager;
    }

    public void sendCallEvent(String callId, CallEvent callEvent) {
        CompletableFuture<Void> future = kafkaTemplate.send(outputTopic, callId, callEvent)
                .thenAccept(result -> log.info("Sent CallEvent with key: {} and offset: {}", callId, result.getRecordMetadata().offset()))
                .exceptionally(ex -> {
                    log.error("Failed to send CallEvent with key: {} - {}", callId, ex.getMessage());
                    return null;
                });
    }

    private CallEvent createBaseCallEvent(String callId, String eventName, long unixTimeMs, String direction,
                                          String from, String callerIdNumber, String to, String presenceId,
                                          String toUri, String fromUri) {
        CallEvent event = new CallEvent();
        event.setCallID(callId);
        event.setEventName(eventName);
        event.setUnixTimeMs(unixTimeMs);
        event.setTimestamp(unixTimeMs / 1000);
        event.setCallDirection(direction);
        event.setFrom(from);
        event.setFromUri(fromUri);
        event.setCallerIDNumber(callerIdNumber);
        event.setCallerIDName(callerIdNumber);
        event.setTo(to);
        event.setToUri(toUri);
        event.setRequest(to);
        event.setCalleeIDNumber(toUri.split("@")[0]);
        event.setCalleeIDName(toUri.split("@")[0]);
        event.setPresenceID(presenceId);
        event.setChannelCreatedTime(unixTimeMs * 1000);
        return event;
    }

    public void simulateSingleSuccessfulCall(
            String callId,
            String callerNumber,
            String calleeNumber,
            String queueId,
            String requestedAgentId, // manager will assign available
            long startTime,
            int callDuration // in seconds
    ) {
        log.info("[{}] Starting simulation...", callId);

        String presenceId = callerNumber + "@10.64.157.4";
        String toUri = calleeNumber + "@10.52.125.34";
        String fromUri = callerNumber + "@10.64.157.4";

        long currentTime = startTime;
        MockAgent assignedAgent = null; // Agent actually assigned to this call

        try {
            // CHANNEL_CREATE (Inbound Leg)
            CallEvent createEvent = createBaseCallEvent(callId, "CHANNEL_CREATE", currentTime, "inbound", callerNumber, callerNumber, calleeNumber, presenceId, toUri, fromUri);
            createEvent.setQueueId(queueId);
            createEvent.setVcNumber(calleeNumber);
            sendCallEvent(callId, createEvent);
            sleep(50); // Small delay before next event

            // CHANNEL_QUEUE (Inbound Leg)
            currentTime += random.nextInt(500) + 100;
            CallEvent queueEvent = createBaseCallEvent(callId, "CHANNEL_QUEUE", currentTime, "inbound", callerNumber, callerNumber, calleeNumber, presenceId, toUri, fromUri);
            queueEvent.setQueueId(queueId);
            queueEvent.setVcNumber(calleeNumber);
            sendCallEvent(callId, queueEvent);
            log.info("[{}] Call entered queue {}.", callId, queueId);

            // --- Agent Assignment Logic ---
            long queueEntryTime = currentTime;
            long queueWaitTime = 0;
            int maxQueueWaitMs = 30000; // Max 30 seconds wait in queue before giving up
            boolean agentAssigned = false;

            while (!agentAssigned && (currentTime - queueEntryTime < maxQueueWaitMs)) {
                Optional<MockAgent> availableAgentOpt = agentManager.tryAssignCallToAgent(queueId, callId);
                if (availableAgentOpt.isPresent()) {
                    assignedAgent = availableAgentOpt.get();
                    agentAssigned = true;
                    queueWaitTime = currentTime - queueEntryTime;
                    log.info("[{}] Assigned to agent {} after {} ms in queue.", callId, assignedAgent.getAgentId(), queueWaitTime);
                    break;
                } else {
                    // No agent available, keep call in queue
                    sleep(200 + random.nextInt(100)); // Simulate checking for agent every 200-300ms
                    currentTime = System.currentTimeMillis(); // Update current time to reflect real wait
                    log.debug("[{}] Waiting for agent in queue {}. Current wait: {}ms", callId, queueId, currentTime - queueEntryTime);
                }
            }

            if (!agentAssigned) {
                log.warn("[{}] No agent available within {}ms. Call abandoned or routed differently.", callId, maxQueueWaitMs);
                // Simulate call destroy due to abandonment/timeout
                currentTime = System.currentTimeMillis(); // Use actual current time for destroy
                CallEvent destroyAbandoned = createBaseCallEvent(callId, "CHANNEL_DESTROY", currentTime, "inbound", callerNumber, callerNumber, calleeNumber, presenceId, toUri, fromUri);
                destroyAbandoned.setHangupCause("NO_ANSWER");
                destroyAbandoned.setQueueId(queueId);
                destroyAbandoned.setVcNumber(calleeNumber);
                sendCallEvent(callId, destroyAbandoned);
                return; // End simulation for this call
            }

            // CHANNEL_CREATE (Outbound Leg - Agent's Phone)
            currentTime += random.nextInt(100) + 50;
            String agentLegCallId = "AGENT_LEG-" + UUID.randomUUID().toString();
            CallEvent agentCreateEvent = createBaseCallEvent(agentLegCallId, "CHANNEL_CREATE", currentTime, "outbound", "nouser@cc.ipcc.vn", callerNumber, assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", "nouser@cc.ipcc.vn");
            agentCreateEvent.setOtherLegCallID(callId);
            agentCreateEvent.setQueueId(queueId);
            agentCreateEvent.setVcNumber(calleeNumber);
            sendCallEvent(agentLegCallId, agentCreateEvent);
            sleep(50);

            // CHANNEL_ANSWER (Inbound Leg)
            currentTime += random.nextInt(500) + 100;
            CallEvent answerInboundEvent = createBaseCallEvent(callId, "CHANNEL_ANSWER", currentTime, "inbound", callerNumber, callerNumber, calleeNumber, presenceId, toUri, fromUri);
            answerInboundEvent.setQueueId(queueId);
            answerInboundEvent.setVcNumber(calleeNumber);
            sendCallEvent(callId, answerInboundEvent);
            sleep(50);

            // CHANNEL_ANSWER (Outbound Leg - Agent's Phone)
            currentTime += random.nextInt(100);
            CallEvent answerOutboundEvent = createBaseCallEvent(agentLegCallId, "CHANNEL_ANSWER", currentTime, "outbound", "nouser@cc.ipcc.vn", callerNumber, assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", "nouser@cc.ipcc.vn");
            answerOutboundEvent.setOtherLegCallID(callId);
            answerOutboundEvent.setQueueId(queueId);
            answerOutboundEvent.setVcNumber(calleeNumber);
            sendCallEvent(agentLegCallId, answerOutboundEvent);
            sleep(50);

            // CHANNEL_BRIDGE
            currentTime += random.nextInt(100) + 10;
            CallEvent bridgeEvent = createBaseCallEvent(agentLegCallId, "CHANNEL_BRIDGE", currentTime, "outbound", "nouser@cc.ipcc.vn", callerNumber, assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", "nouser@cc.ipcc.vn");
            bridgeEvent.setOtherLegCallID(callId);
            bridgeEvent.setOtherLegDirection("inbound");
            bridgeEvent.setOtherLegDestinationNumber(calleeNumber);
            bridgeEvent.setQueueId(queueId);
            bridgeEvent.setVcNumber(calleeNumber);
            sendCallEvent(agentLegCallId, bridgeEvent);
            log.info("[{}] Call bridged. Agent: {}. Talking for {}s", callId, assignedAgent.getAgentId(), callDuration);
            sleep(50);

            // TALKING DURATION
            sleep(callDuration * 1000L);
            currentTime += callDuration * 1000L;

            // CHANNEL_UNBRIDGE (Agent Leg Hangs Up First)
            currentTime += random.nextInt(50) + 10;
            CallEvent unbridgeAgentEvent = createBaseCallEvent(agentLegCallId, "CHANNEL_UNBRIDGE", currentTime, "outbound", "Unknown@cc.ipcc.vn", callerNumber, assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", "nouser@cc.ipcc.vn");
            unbridgeAgentEvent.setOtherLegCallID(callId);
            unbridgeAgentEvent.setOtherLegDirection("inbound");
            unbridgeAgentEvent.setOtherLegDestinationNumber(calleeNumber);
            unbridgeAgentEvent.setHangupCode("sip:200");
            unbridgeAgentEvent.setHangupCause("NORMAL_CLEARING");
            unbridgeAgentEvent.setQueueId(queueId);
            unbridgeAgentEvent.setVcNumber(calleeNumber);
            sendCallEvent(agentLegCallId, unbridgeAgentEvent);
            sleep(50);

            // CHANNEL_DESTROY (Agent Leg)
            currentTime += random.nextInt(50) + 10;
            CallEvent destroyAgentEvent = createBaseCallEvent(agentLegCallId, "CHANNEL_DESTROY", currentTime, "outbound", "Unknown@cc.ipcc.vn", callerNumber, assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", assignedAgent.getAgentId() + "@cc.ipcc.vn", "nouser@cc.ipcc.vn");
            destroyAgentEvent.setOtherLegCallID(callId);
            destroyAgentEvent.setOtherLegDirection("inbound");
            destroyAgentEvent.setOtherLegDestinationNumber(calleeNumber);
            destroyAgentEvent.setHangupCode("sip:200");
            destroyAgentEvent.setHangupCause("NORMAL_CLEARING");
            destroyAgentEvent.setQueueId(queueId);
            destroyAgentEvent.setVcNumber(calleeNumber);
            sendCallEvent(agentLegCallId, destroyAgentEvent);
            sleep(50);

            // CHANNEL_DESTROY (Inbound Leg)
            currentTime += random.nextInt(50) + 10;
            CallEvent destroyInboundEvent = createBaseCallEvent(callId, "CHANNEL_DESTROY", currentTime, "inbound", callerNumber, callerNumber, calleeNumber, presenceId, toUri, fromUri);
            destroyInboundEvent.setOtherLegCallID(agentLegCallId);
            destroyInboundEvent.setOtherLegDirection("outbound");
            destroyInboundEvent.setOtherLegDestinationNumber(assignedAgent.getAgentId());
            destroyInboundEvent.setHangupCode("sip:200");
            destroyInboundEvent.setHangupCause("NORMAL_CLEARING");
            destroyInboundEvent.setQueueId(queueId);
            destroyInboundEvent.setVcNumber(calleeNumber);
            sendCallEvent(callId, destroyInboundEvent);

        } finally {
            if (assignedAgent != null) {
                agentManager.releaseAgent(assignedAgent.getAgentId()); // Ensure agent is released even if errors occur
            }
            log.info("[{}] Finished simulation.", callId);
        }
    }

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Simulation sleep interrupted", e);
        }
    }
}
