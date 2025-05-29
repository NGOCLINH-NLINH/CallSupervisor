package org.example.calleventproducerservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallEvent {
    @JsonProperty("Call-ID")
    private String callID;

    @JsonProperty("Event-Name")
    private String eventName;

    @JsonProperty("Unix-Time-Ms")
    private Long unixTimeMs;

    @JsonProperty("Timestamp")
    private Long timestamp;

    @JsonProperty("Request")
    private String request;

    @JsonProperty("Presence-ID")
    private String presenceID;

    @JsonProperty("From-Uri")
    private String fromUri;

    @JsonProperty("From")
    private String from;

    @JsonProperty("Caller-ID-Number")
    private String callerIDNumber;

    @JsonProperty("Caller-ID-Name")
    private String callerIDName;

    @JsonProperty("Call-Direction")
    private String callDirection;

    @JsonProperty("To-Uri")
    private String toUri;

    @JsonProperty("To")
    private String to;

    @JsonProperty("Callee-ID-Number")
    private String calleeIDNumber;

    @JsonProperty("Callee-ID-Name")
    private String calleeIDName;

    @JsonProperty("Other-Leg-Call-ID")
    private String otherLegCallID;

    @JsonProperty("Other-Leg-Direction")
    private String otherLegDirection;

    @JsonProperty("Other-Leg-Destination-Number")
    private String otherLegDestinationNumber;

    @JsonProperty("Hangup-Code")
    private String hangupCode;

    @JsonProperty("Hangup-Cause")
    private String hangupCause;

    @JsonProperty("Channel-Created-Time")
    private Long channelCreatedTime;

    public boolean isCreateEvent() {
        return "CHANNEL_CREATE".equals(this.eventName);
    }

    public boolean isAnswerEvent() {
        return "CHANNEL_ANSWER".equals(this.eventName);
    }

    public boolean isBridgeEvent() {
        return "CHANNEL_BRIDGE".equals(this.eventName);
    }

    public boolean isUnbridgeEvent() {
        return "CHANNEL_UNBRIDGE".equals(this.eventName);
    }

    public boolean isHoldEvent() {
        return "CHANNEL_HOLD".equals(this.eventName);
    }

    public boolean isUnholdEvent() {
        return "CHANNEL_UNHOLD".equals(this.eventName);
    }

    public boolean isChannelQueueEvent() {
        return "CHANNEL_QUEUE".equals(this.eventName);
    }

    public boolean isDestroyEvent() {
        return "CHANNEL_DESTROY".equals(this.eventName);
    }

    public boolean isTransferorEvent() {
        return "CHANNEL_TRANSFEROR".equals(eventName);
    }

    public boolean isTransfereeEvent() {
        return "CHANNEL_TRANSFEREE".equals(eventName);
    }
}
