package org.example.calleventproducerservice.controller;

import org.example.calleventproducerservice.service.CallEventProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("api/v1/call-events")
@EnableScheduling
public class CallEventController {
    private final CallEventProducerService callEventProducerService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    public CallEventController(CallEventProducerService callEventProducerService) {
        this.callEventProducerService = callEventProducerService;
    }

    @GetMapping("/simulate-single")
    public ResponseEntity<String> simulateSingleCall(
            @RequestParam(defaultValue = "0969389999") String caller,
            @RequestParam(defaultValue = "1068") String callee,
            @RequestParam(required = false) String agent,
            @RequestParam(defaultValue = "15") int duration
    ) {
        String callId = "SIM-" + UUID.randomUUID().toString();
        String queueId = CallEventProducerService.QUEUE_IDS.get(CallEventProducerService.random.nextInt(CallEventProducerService.QUEUE_IDS.size()));

        if (agent == null || !CallEventProducerService.AGENT_IDS.contains(agent)) {
            agent = CallEventProducerService.AGENT_IDS.get(CallEventProducerService.random.nextInt(CallEventProducerService.AGENT_IDS.size()));
        }

        final String agentFinal = agent;

        long startTime = System.currentTimeMillis();

        executorService.submit(() -> {
            callEventProducerService.simulateSingleSuccessfulCall(
                    callId,
                    caller,
                    callee,
                    queueId,
                    agentFinal,
                    startTime,
                    duration
            );
        });
        return ResponseEntity.ok("Simulating single call: " + callId);
    }

    @GetMapping("/simulate-multiple")
    public ResponseEntity<String> simulateMultipleCalls(
            @RequestParam(defaultValue = "50") int count,
            @RequestParam(defaultValue = "30") int maxDuration
    ) {
        for (int i = 0; i < count; i++) {
            String caller = "0" + (900000000 + CallEventProducerService.random.nextInt(100000000));
            String callee = CallEventProducerService.VC_NUMBERS.get(CallEventProducerService.random.nextInt(CallEventProducerService.VC_NUMBERS.size()));
            String agent = CallEventProducerService.AGENT_IDS.get(CallEventProducerService.random.nextInt(CallEventProducerService.AGENT_IDS.size()));
            int duration = CallEventProducerService.random.nextInt(maxDuration) + 5;
            String callId = "MULTI-SIM-" + UUID.randomUUID().toString();
            String queueId = CallEventProducerService.QUEUE_IDS.get(CallEventProducerService.random.nextInt(CallEventProducerService.QUEUE_IDS.size()));

            long startTime = System.currentTimeMillis();

            executorService.submit(() -> {
                callEventProducerService.simulateSingleSuccessfulCall(
                        callId,
                        caller,
                        callee,
                        queueId,
                        agent,
                        startTime,
                        duration
                );
            });
            try {
                Thread.sleep(CallEventProducerService.random.nextInt(500) + 100); // 100-600ms between new call starts
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return ResponseEntity.ok(String.format("Started simulating %d concurrent calls.", count));
    }
}
