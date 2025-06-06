package org.example.agentserver.repository;

import org.example.commonmodel.model.ChannelEvent;
import org.example.commonmodel.model.State;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class LiveChannelEventRepository {
    private final ConcurrentMap<String, ChannelEvent> liveCalls = new ConcurrentHashMap<>();

    public void save(ChannelEvent event) {
        if (event.getState() == State.DESTROYED) {
            liveCalls.remove(event.getCallId());
        } else {
            liveCalls.put(event.getCallId(), event);
        }
    }

    public ChannelEvent findByCallId(String callId) {
        return liveCalls.get(callId);
    }

    public Collection<ChannelEvent> findAllActiveCalls() {
        return liveCalls.values().stream()
                .filter(event -> event.getState() != State.DESTROYED)
                .collect(Collectors.toList());
    }

    public Collection<ChannelEvent> findCallsByQueueId(String queueId) {
        return liveCalls.values().stream()
                .filter(event -> event.getQueueId().equalsIgnoreCase(queueId))
                .filter(event -> event.getState() != State.DESTROYED)
                .collect(Collectors.toList());
    }

    public Collection<ChannelEvent> findCallsByCallerNumber(String callerNumber) {
        return liveCalls.values().stream()
                .filter(event -> event.getCallerIdNumber() != null && event.getCallerIdNumber().contains(callerNumber)) // Dùng contains để tìm kiếm một phần
                .filter(event -> event.getState() != State.DESTROYED)
                .collect(Collectors.toList());
    }

    public Collection<ChannelEvent> findCallsByAgentId(String agentId) {
        return liveCalls.values().stream()
                .filter(event -> event.getOwnerId() != null && event.getOwnerId().equalsIgnoreCase(agentId))
                .filter(event -> event.getState() != State.DESTROYED)
                .collect(Collectors.toList());
    }

    public Collection<ChannelEvent> findCallsByVcNumber(String VcNumber) {
        return liveCalls.values().stream()
                .filter(event -> event.getVcNumber().equalsIgnoreCase(VcNumber))
                .filter(event -> event.getState() != State.DESTROYED)
                .collect(Collectors.toList());
    }

    public int getActiveCallCount() {
        return (int)liveCalls.values().stream()
                .filter(event -> event.getState() != State.DESTROYED)
                .count();
    }

    public long getTotalCalls() {
        return liveCalls.size();
    }

    public long getCallsByState(State state) {
        return liveCalls.values().stream()
                .filter(event -> event.getState() == state)
                .count();
    }

    public double getAverageWaitTimeInQueue() {
        return liveCalls.values().stream()
                .filter(event -> event.getQueueId() != null && event.getRingingDuration() != null && event.getState() == State.ANSWERED)
                .mapToLong(ChannelEvent::getRingingDuration)
                .average()
                .orElse(0.0);
    }
}
