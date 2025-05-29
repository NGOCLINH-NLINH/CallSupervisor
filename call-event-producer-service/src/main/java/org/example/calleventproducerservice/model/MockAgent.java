package org.example.calleventproducerservice.model;

import lombok.Getter;

public class MockAgent {
    public enum AgentState {
        AVAILABLE,
        BUSY
    }

    @Getter
    private String agentId;
    @Getter
    private AgentState state;
    private String currentCallId;
    private long stateChangeTime;

    public MockAgent(String agentId) {
        this.agentId = agentId;
        this.state = AgentState.AVAILABLE;
        this.stateChangeTime = System.currentTimeMillis();
    }

    public boolean isAvailable() {
        return this.state == AgentState.AVAILABLE;
    }

    public void assignCall(String callId) {
        if (isAvailable()) {
            this.state = AgentState.BUSY;
            this.currentCallId = callId;
            this.stateChangeTime = System.currentTimeMillis();
        } else {
            throw new IllegalStateException("Agent " + agentId + " is not available to assign call " + callId);
        }
    }

    public void releaseCall() {
        this.state = AgentState.AVAILABLE;
        this.currentCallId = null;
        this.stateChangeTime = System.currentTimeMillis();
    }
}
