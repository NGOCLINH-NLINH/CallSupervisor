package org.example.agentserver.controller;

import org.example.agentserver.service.AgentDashboardService;
import org.example.commonmodel.model.ChannelEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("api/v1/dashboard")
public class AgentDashboardController {
    private final AgentDashboardService dashboardService;

    public AgentDashboardController(AgentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/live-calls")
    public ResponseEntity<Collection<ChannelEvent>> getLiveCalls(
            @RequestParam(required = false) String queueId,
            @RequestParam(required = false) String vcNumber,
            @RequestParam(required = false) String callerNumber,
            @RequestParam(required = false) String agentId) {
        Collection<ChannelEvent> liveCalls = dashboardService.getLiveCalls(queueId, vcNumber, callerNumber, agentId);
        return ResponseEntity.ok(liveCalls);
    }

    @GetMapping("/active-call-count")
    public ResponseEntity<Integer> getActiveCallCount() {
        return ResponseEntity.ok(dashboardService.getActiveCallCount());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> stats = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }
}
