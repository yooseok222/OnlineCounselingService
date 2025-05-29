package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.agent.service.AgentStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractStatusController {

    private final AgentStatusService agentStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 세션 ID 저장을 위한 Map (여러 세션 지원)
    private ConcurrentHashMap<String, Date> activeSessions = new ConcurrentHashMap<>();
    
    // 현재 활성 세션 ID (가장 최근에 생성된 세션)
    private volatile String currentActiveSessionId = null;

    /**
     * 상담원 입장 상태 확인 (대기실에서 폴링 용도)
     * @return 상담원 입장 상태와 세션 정보
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getContractStatus() {
        boolean isPresent = agentStatusService.isAgentPresent();
        log.info("계약/상담 상태 확인 요청 - 현재 상태: {}", isPresent ? "입장" : "미입장");
        
        Map<String, Object> response = Map.of("present", isPresent);
        
        // 현재 활성 세션 ID를 응답에 추가
        if (currentActiveSessionId != null) {
            response = Map.of(
                "present", isPresent,
                "sessionId", currentActiveSessionId
            );
            log.info("현재 활성 세션 ID 응답: {}", currentActiveSessionId);
        } else if (!activeSessions.isEmpty()) {
            // 백업: 가장 최근 세션 선택
            String latestSessionId = getLatestSessionId();
            if (latestSessionId != null) {
                response = Map.of(
                    "present", isPresent,
                    "sessionId", latestSessionId
                );
                currentActiveSessionId = latestSessionId; // 현재 활성 세션으로 설정
                log.info("백업 세션 ID 응답: {}", latestSessionId);
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 상담원 입장 상태 설정
     * @param status 상담원 상태 객체
     * @return 결과 메시지
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> setContractStatus(@RequestBody Map<String, Object> status) {
        Boolean present = (Boolean) status.get("present");
        String sessionId = (String) status.get("sessionId");
        
        log.info("상담원 상태 업데이트 요청 - 상태: {}, 세션ID: {}", 
                present ? "입장" : "퇴장", sessionId);
        
        if (present != null) {
            agentStatusService.setAgentPresent(present);
            
            // 세션 ID가 포함되어 있다면 저장
            if (sessionId != null && !sessionId.isEmpty()) {
                if (present) {
                    // 입장 상태이면 세션 추가
                    activeSessions.put(sessionId, new Date());
                    currentActiveSessionId = sessionId; // 현재 활성 세션으로 설정
                    log.info("세션 활성화: {} (현재 활성 세션으로 설정)", sessionId);
                } else {
                    // 퇴장 상태이면 해당 세션 제거
                    activeSessions.remove(sessionId);
                    // 현재 활성 세션이 제거되는 세션이면 null로 설정
                    if (sessionId.equals(currentActiveSessionId)) {
                        currentActiveSessionId = null;
                        log.info("현재 활성 세션 해제: {}", sessionId);
                    }
                    log.info("세션 비활성화: {}", sessionId);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "상담원 상태가 " + (present ? "입장" : "퇴장") + " 으로 변경되었습니다.",
                "sessionId", sessionId != null ? sessionId : ""
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "유효하지 않은 요청입니다."
            ));
        }
    }

    /**
     * 상담원 입장 상태 설정 (sendBeacon 지원 - Content-Type: text/plain 또는 application/json)
     * @param request HTTP 요청
     * @return 결과 메시지
     */
    @PostMapping(value = "/status", consumes = {"text/plain", "application/json"})
    public ResponseEntity<Map<String, Object>> setContractStatusFromBeacon(
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            String contentType = request.getContentType();
            log.info("sendBeacon 요청 받음 - Content-Type: {}", contentType);
            
            // 요청 본문을 문자열로 읽기
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                stringBuilder.append(line);
            }
            String requestBody = stringBuilder.toString();
            log.info("sendBeacon 요청 본문: {}", requestBody);
            
            if (requestBody.isEmpty()) {
                log.warn("sendBeacon 요청 본문이 비어있음");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "요청 본문이 비어있습니다."
                ));
            }
            
            Map<String, Object> status;
            
            // JSON 파싱
            try {
                status = objectMapper.readValue(requestBody, Map.class);
                log.info("JSON 파싱 성공: {}", status);
            } catch (Exception parseError) {
                log.error("JSON 파싱 실패: {}, 원본 요청: {}", parseError.getMessage(), requestBody);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "JSON 파싱 실패: " + parseError.getMessage()
                ));
            }
            
            // 기존 로직 재사용
            return setContractStatus(status);
            
        } catch (Exception e) {
            log.error("sendBeacon 요청 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "요청 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
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
} 