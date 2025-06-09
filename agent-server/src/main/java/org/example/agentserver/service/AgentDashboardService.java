package org.example.agentserver.service;

import org.example.agentserver.repository.LiveChannelEventRepository;
import org.example.commonmodel.model.ChannelEvent;
import org.example.commonmodel.model.State;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentDashboardService {
    private final LiveChannelEventRepository repository;

    public AgentDashboardService(LiveChannelEventRepository repository) {
        this.repository = repository;
    }

    public Collection<ChannelEvent> getLiveCalls(String queueId, String vcNumber, String callerNumber, String agentId) {
        Collection<ChannelEvent> filteredCalls = repository.findAllActiveCalls();

        if (queueId != null && !queueId.isEmpty()) {
            filteredCalls = repository.findCallsByQueueId(queueId);
        }

        if (vcNumber != null && !vcNumber.isEmpty()) {
            filteredCalls = repository.findCallsByVcNumber(vcNumber);
        }

        if (callerNumber != null && !callerNumber.isEmpty()) {
            filteredCalls = repository.findCallsByCallerNumber(callerNumber);
        }

        if (agentId != null && !agentId.isEmpty()) {
            filteredCalls = repository.findCallsByAgentId(agentId);
        }

        return filteredCalls;
    }

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalCalls = repository.getTotalCalls();
        long answeredCalls = repository.getCallsByState(State.ANSWERED);
        long bridgedCalls = repository.getCallsByState(State.BRIDGED);
        long queuedCalls = repository.getCallsByState(State.QUEUED);
        long createdCalls = repository.getCallsByState(State.CREATED);
        long destroyedCalls = repository.getCallsByState(State.DESTROYED);

        double avgWaitTime = repository.getAverageWaitTimeInQueue();

        stats.put("totalCalls", totalCalls);
        stats.put("answeredCalls", answeredCalls);
        stats.put("bridgedCalls", bridgedCalls);
        stats.put("queuedCalls", queuedCalls);
        stats.put("createdCalls", createdCalls);
        stats.put("destroyedCalls", destroyedCalls);
        stats.put("avgWaitTime", avgWaitTime);

        return stats;
    }

    public int getActiveCallCount() {
        return repository.getActiveCallCount();
    }
}
