package kr.or.kosa.visang.domain.agent.controller;

import kr.or.kosa.visang.domain.agent.service.AgentStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentStatusController {

    private final AgentStatusService agentStatusService;

    /**
     * 상담원 상태 확인 (디버깅용)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        boolean isPresent = agentStatusService.isAgentPresent();
        int activeCount = agentStatusService.getActiveAgentCount();
        String statusInfo = agentStatusService.getStatusInfo();
        
        log.info("상담원 상태 조회 API 호출: {}", statusInfo);
        
        return ResponseEntity.ok(Map.of(
            "present", isPresent,
            "activeCount", activeCount,
            "statusInfo", statusInfo
        ));
    }

    /**
     * 상담원 상태 강제 리셋 (디버깅용)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetAgentStatus() {
        String beforeStatus = agentStatusService.getStatusInfo();
        agentStatusService.resetAgentStatus();
        String afterStatus = agentStatusService.getStatusInfo();
        
        log.info("상담원 상태 리셋 API 호출");
        log.info("리셋 전: {}", beforeStatus);
        log.info("리셋 후: {}", afterStatus);
        
        return ResponseEntity.ok(Map.of(
            "message", "상담원 상태가 리셋되었습니다",
            "before", beforeStatus,
            "after", afterStatus
        ));
    }
} 