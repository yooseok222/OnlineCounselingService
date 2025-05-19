package kr.or.kosa.visang.domain.agent.controller;

import kr.or.kosa.visang.domain.agent.service.AgentStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/contract")
public class AgentStatusController {

    @Autowired
    private AgentStatusService agentStatusService;

    // 세션 ID 저장을 위한 Map (여러 세션 지원)
    private ConcurrentHashMap<String, Date> activeSessions = new ConcurrentHashMap<>();

    /**
     * 상담원 입장 상태 확인
     * @return 상담원 입장 상태
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        boolean isPresent = agentStatusService.isAgentPresent();
        System.out.println("상담원 상태 확인 요청 - 현재 상태: " + (isPresent ? "입장" : "미입장"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("present", isPresent);
        
        // 최근 세션 ID를 응답에 추가
        if (!activeSessions.isEmpty()) {
            // 가장 최근 세션 선택 (마지막으로 업데이트된 세션)
            String latestSessionId = getLatestSessionId();
            if (latestSessionId != null) {
                response.put("sessionId", latestSessionId);
                System.out.println("세션 ID 응답: " + latestSessionId);
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 가장 최근 세션 ID 찾기
     */
    private String getLatestSessionId() {
        if (activeSessions.isEmpty()) return null;
        
        String latestSessionId = null;
        Date latestTime = new Date(0);
        
        for (Map.Entry<String, Date> entry : activeSessions.entrySet()) {
            if (entry.getValue().after(latestTime)) {
                latestTime = entry.getValue();
                latestSessionId = entry.getKey();
            }
        }
        
        return latestSessionId;
    }

    /**
     * 상담원 입장 상태 설정
     * @param status 상담원 상태 객체
     * @return 결과 메시지
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> setAgentStatus(@RequestBody Map<String, Object> status) {
        Boolean present = (Boolean) status.get("present");
        String sessionId = (String) status.get("sessionId");
        
        System.out.println("상담원 상태 업데이트 요청 - 상태: " + (present ? "입장" : "퇴장") + ", 세션ID: " + sessionId);
        
        if (present != null) {
            agentStatusService.setAgentPresent(present);
            
            // 세션 ID가 포함되어 있다면 저장
            if (sessionId != null && !sessionId.isEmpty()) {
                if (present) {
                    // 입장 상태이면 세션 추가
                    activeSessions.put(sessionId, new Date());
                    System.out.println("세션 활성화: " + sessionId);
                } else {
                    // 퇴장 상태이면 해당 세션 제거
                    activeSessions.remove(sessionId);
                    System.out.println("세션 비활성화: " + sessionId);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상담원 상태가 " + (present ? "입장" : "퇴장") + " 으로 변경되었습니다.");
            
            // 세션 정보도 응답에 포함
            if (sessionId != null) {
                response.put("sessionId", sessionId);
            }
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "유효하지 않은 요청입니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }
} 