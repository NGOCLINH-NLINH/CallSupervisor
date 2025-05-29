package org.example.calleventproducerservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
public class MockAgentManager {
    @Data
    @AllArgsConstructor
    private static class PendingCall {
        String callId;
        String queueId;
    }

    private static final Logger log = LoggerFactory.getLogger(MockAgentManager.class);
    private final Map<String, MockAgent> agents;
    private final Map<String, MockQueue> queues;

    private final Queue<PendingCall> pendingCalls;

    public void addAgents(List<String> agentIds) {
        agentIds.forEach(id -> this.agents.putIfAbsent(id, new MockAgent(id)));
    }

    public void addQueues(List<String> queueIds) {
        queueIds.forEach(id -> this.queues.putIfAbsent(id, new MockQueue(id)));
    }

    public MockAgentManager(List<String> agentIds, List<String> queueIds) {
        this.agents = agentIds.stream()
                .collect(Collectors.toConcurrentMap(
                        agentId -> agentId,
                        MockAgent::new
                ));
        this.queues = queueIds.stream()
                .collect(Collectors.toConcurrentMap(
                        queueId -> queueId,
                        MockQueue::new
                ));
        this.pendingCalls = new ConcurrentLinkedQueue<>();
        log.info("MockAgentManager initialized with {} agents and {} queues.", agents.size(), queues.size());
    }

    public Optional<MockAgent> tryAssignCallToAgent(String queueId, String callId) {
        MockQueue mockQueue = queues.get(queueId);
        if (mockQueue == null) {
            log.warn("Attempted to assign call {} to non-existent queue {}", callId, queueId);
            return Optional.empty();
        }

        Optional<MockAgent> availableAgent = agents.values().stream()
                .filter(MockAgent::isAvailable)
                .findAny();

        if (availableAgent.isPresent()) {
            MockAgent agent = availableAgent.get();
            try {
                agent.assignCall(callId);
                mockQueue.dequeueCall();
                log.info("Call {} assigned to agent {}", callId, agent.getAgentId());
                return Optional.of(agent);
            } catch (IllegalStateException e) {
                log.warn("Agent {} became unavailable for call {}. Retrying or re-enqueueing.", agent.getAgentId(), callId);
                return Optional.empty();
            }
        } else {
            log.debug("No available agents for queue {}. Call {} remains in queue.", queueId, callId);
            return Optional.empty();
        }
    }

    public void releaseAgent(String agentId) {
        MockAgent agent = agents.get(agentId);
        if (agent != null && agent.getState() == MockAgent.AgentState.BUSY) {
            agent.releaseCall();
            log.info("Agent {} released from", agentId);
        }
    }

    @Scheduled(fixedRate = 1000) // Run every 1 second
    public void processAgentStatesAndQueues() {
        long currentTime = System.currentTimeMillis();

        // Try to assign pending calls
        while (!pendingCalls.isEmpty()) {
            PendingCall pc = pendingCalls.peek();
            Optional<MockAgent> assignedAgent = tryAssignCallToAgent(pc.getQueueId(), pc.getCallId());
            if (assignedAgent.isPresent()) {
                pendingCalls.poll();
            } else {
                break;
            }
        }

        // Try to assign calls from queues to AVAILABLE agents
        agents.values().stream()
                .filter(MockAgent::isAvailable)
                .forEach(agent -> {
                    Optional<MockQueue> targetQueue = queues.values().stream()
                            .filter(q -> !q.isEmpty())
                            .findFirst();

                    if (targetQueue.isPresent()) {
                        MockQueue queue = targetQueue.get();
                        String callId = queue.dequeueCall();
                        if (callId != null) {
                            try {
                                agent.assignCall(callId);
                                log.info("Scheduled assignment: Call {} from queue {} assigned to agent {}", callId, queue.getQueueId(), agent.getAgentId());
                            } catch (IllegalStateException e) {
                                log.warn("Agent {} became unavailable during scheduled assignment for call {}.", agent.getAgentId(), callId);
                                queue.enqueueCall(callId); // Re-enqueue if assignment fails
                            }
                        }
                    }
                });

    }
}
