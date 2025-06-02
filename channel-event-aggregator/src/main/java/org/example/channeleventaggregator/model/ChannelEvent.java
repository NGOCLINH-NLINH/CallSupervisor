package org.example.channeleventaggregator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.example.commonmodel.model.CallEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelEvent {
    // Basic Call Info
    private String callId;
    private String otherLegCallId;
    private String otherLegDirection;
    private String otherLegDestinationNumber;

    // Caller/Callee Info
    private String from;
    private String fromUri;
    private String callerIdName;
    private String callerIdNumber;
    private String to;
    private String toUri;
    private String request;
    private String requestNumber;
    private String dialedNumber;
    private String calleeIdNumber;
    private String direction;

    // Agent/Queue Info
    private String queueId;
    private String vcNumber;
    private String presenceId;

    // Timestamps and Durations
    private Long createdTime;
    private Long answeredTime;
    private Long bridgedTime;
    private Long unbridgedTime;
    private Long destroyedTime;
    private Integer totalDuration; // in seconds
    private Integer ringingDuration; // in seconds
    private Integer talkingDuration; // in seconds
    private Long timestamp; // Last event timestamp

    // Call State Management
    private State state;
    private int holdCount;
    private long holdDuration; // in seconds
    private boolean holding;
    private Long lastHoldTime;
    private Long lastUnholdTime;
    private boolean wasQueued;
    private String firstQueueId;
    private Long firstQueuedTime;
    private boolean channelTransferor;
    private boolean channelTransferee;

    // Hangup Details
    private String hangupCode;
    private String hangupCause;

    private static final String INBOUND = "inbound";

    public ChannelEvent with(CallEvent callEvt) {
        long eventTimeMs = callEvt.getUnixTimeMs();

        createdTime = ObjectUtils.firstNonNull(createdTime, callEvt.getChannelCreatedTime());
        callId = callId == null ? callEvt.getCallID() : callId;

        presenceId = ObjectUtils.firstNonNull(presenceId, callEvt.getPresenceID());

        otherLegCallId = callEvt.getOtherLegCallID();
        otherLegDirection = callEvt.getOtherLegDirection();
        otherLegDestinationNumber = callEvt.getOtherLegDestinationNumber();

        callerIdName = Optional.ofNullable(callerIdName).orElse(callEvt.getCallerIDName());
        callerIdNumber = Optional.ofNullable(callerIdNumber).orElse(callEvt.getCallerIDNumber());

        from = callEvt.getFrom();
        fromUri = callEvt.getFromUri();
        to = (to == null || to.equals("") || to.contains("nouser")) ? callEvt.getTo() : to;
        dialedNumber = to != null ? to.split("@")[0] : Optional.ofNullable(calleeIdNumber).orElse(callEvt.getCalleeIDNumber());
        request = callEvt.getRequest();
        requestNumber = request != null ? request.split("@")[0] : null;
        calleeIdNumber = requestNumber;
        toUri = callEvt.getToUri();
        direction = direction == null ? callEvt.getCallDirection() : direction;

        queueId = ObjectUtils.firstNonNull(queueId, callEvt.getQueueId());
        vcNumber = ObjectUtils.firstNonNull(vcNumber, callEvt.getVcNumber());

        if (callEvt.isCreateEvent()) {
            createdTime = ObjectUtils.firstNonNull(createdTime, eventTimeMs);
            state = State.CREATED;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String createDateStr = dateFormat.format(new Date(createdTime));
            String callIdNumberPart = callerIdNumber != null && callerIdNumber.length() > 5 ? callerIdNumber.substring(0, 5) : (callerIdNumber != null ? callerIdNumber : "UNKNOWN");
            String derivedCallIdPart = callId != null && callId.length() > 5 ? callId.substring(0, 5) : (callId != null ? callId : "UNKNOWN");
        }

        if (callEvt.isAnswerEvent()) {
            answeredTime = eventTimeMs;
            state = State.ANSWERED;
        }

        if (callEvt.isBridgeEvent()) {
            bridgedTime = eventTimeMs;
            state = State.BRIDGED;
        }

        if (callEvt.isUnbridgeEvent()) {
            unbridgedTime = eventTimeMs;
            state = State.UNBRIDGED;
        }

        if (callEvt.isHoldEvent()) {
            if (!holding) {
                holdCount++;
            }
            holding = true;
            lastHoldTime = eventTimeMs;
            lastUnholdTime = null;
            state = State.HELD;
        }

        if (callEvt.isUnholdEvent()) {
            lastUnholdTime = eventTimeMs;
            if (holding && lastHoldTime != null && lastUnholdTime - lastHoldTime > 0) {
                holdDuration += (lastUnholdTime - lastHoldTime) / 1000;
            }
            holding = false;
            state = State.UNHELD;
        }

        if (callEvt.isChannelQueueEvent()) {
            wasQueued = true;
            state = State.QUEUED;
            if (firstQueueId == null) {
                firstQueueId = queueId;
                firstQueuedTime = eventTimeMs;
            }
        }

        if (callEvt.isTransferorEvent()) {
            channelTransferor = true;
        }

        if (callEvt.isTransfereeEvent()) {
            channelTransferee = true;
        }

        if (callEvt.isDestroyEvent()) {
            destroyedTime = eventTimeMs;
            hangupCode = callEvt.getHangupCode();
            hangupCause = callEvt.getHangupCause();
            state = State.DESTROYED;
        }

        this.totalDuration = (createdTime != null && destroyedTime != null) ?
                Long.valueOf(destroyedTime / 1000).intValue() - Long.valueOf(createdTime / 1000).intValue() : null;
        this.ringingDuration = (createdTime != null && answeredTime != null) ?
                Long.valueOf(answeredTime / 1000).intValue() - Long.valueOf(createdTime / 1000).intValue() : null;
        this.talkingDuration = (answeredTime != null && destroyedTime != null) ?
                Long.valueOf(destroyedTime / 1000).intValue() - Long.valueOf(answeredTime / 1000).intValue() : null;
        this.timestamp = eventTimeMs;
        return this;
    }
}
