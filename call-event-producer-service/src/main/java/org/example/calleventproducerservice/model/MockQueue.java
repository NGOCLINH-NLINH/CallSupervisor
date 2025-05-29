package org.example.calleventproducerservice.model;

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockQueue {
    @Getter
    private String queueId;
    private Queue<String> waitingCalls;
    private ConcurrentMap<String, Long> callEntryTimes;

    public MockQueue(String queueId) {
        this.queueId = queueId;
        this.waitingCalls = new LinkedList<>();
        this.callEntryTimes = new ConcurrentHashMap<>();
    }

    public void enqueueCall(String callId) {
        waitingCalls.offer(callId);
        callEntryTimes.put(callId, System.currentTimeMillis());
    }

    public String dequeueCall() {
        String callId = waitingCalls.poll();
        if (callId != null) {
            callEntryTimes.remove(callId);
        }
        return callId;
    }

    public boolean isEmpty() {
        return waitingCalls.isEmpty();
    }

    public int size() {
        return waitingCalls.size();
    }
}
