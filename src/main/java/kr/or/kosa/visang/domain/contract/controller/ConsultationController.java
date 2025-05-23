package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/consultation")
@RequiredArgsConstructor
public class ConsultationController {
    
    private final ContractService contractService;
    
    /**
     * 상담방 생성 또는 참여
     */
    @PostMapping("/room/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            String currentUserRole = SecurityUtil.getCurrentUserRole();
            
            if (currentUserEmail == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("상담방 참여 요청 - 세션: {}, 사용자: {}, 역할: {}", sessionId, currentUserEmail, currentUserRole);
            
            // 상담방 생성 또는 참여
            Contract contract = contractService.joinConsultationRoom(sessionId);
            
            response.put("success", true);
            response.put("message", "상담방에 성공적으로 참여했습니다.");
            response.put("contractId", contract.getContractId());
            response.put("userRole", currentUserRole);
            response.put("userEmail", currentUserEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("상담방 참여 실패 - 세션: {}, 오류: {}", sessionId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "상담방 참여에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 상담 종료
     */
    @PostMapping("/end")
    public ResponseEntity<Map<String, Object>> endConsultation(
            @RequestParam Long contractId,
            @RequestParam String memo) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            
            if (currentUserEmail == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("상담 종료 요청 - 계약 ID: {}, 메모: {}, 사용자: {}", contractId, memo, currentUserEmail);
            
            // 상담 종료 처리
            int result = contractService.endConsultation(contractId, memo);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "상담이 성공적으로 종료되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "상담 종료 처리에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("상담 종료 실패 - 계약 ID: {}, 오류: {}", contractId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "상담 종료에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * PDF 이메일 전송
     */
    @PostMapping("/send-pdf")
    public ResponseEntity<Map<String, Object>> sendPdf(
            @RequestParam Long contractId,
            @RequestParam String clientEmail,
            @RequestParam(required = false) String pdfData) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            
            if (currentUserEmail == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("PDF 이메일 전송 요청 - 계약 ID: {}, 수신자: {}, 발신자: {}", contractId, clientEmail, currentUserEmail);
            
            // PDF 이메일 전송
            contractService.sendPdfToClient(contractId, pdfData, clientEmail);
            
            response.put("success", true);
            response.put("message", "PDF가 성공적으로 전송되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("PDF 이메일 전송 실패 - 계약 ID: {}, 수신자: {}, 오류: {}", contractId, clientEmail, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "PDF 전송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 계약 정보 조회
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<Map<String, Object>> getContract(@PathVariable Long contractId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Contract contract = contractService.getContractById(contractId);
            
            if (contract != null) {
                response.put("success", true);
                response.put("contract", contract);
            } else {
                response.put("success", false);
                response.put("message", "계약을 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계약 조회 실패 - 계약 ID: {}, 오류: {}", contractId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "계약 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 세션 ID로 상담 정보 조회
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getConsultationBySessionId(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("세션 ID로 상담 정보 조회 요청: {}", sessionId);
            
            Contract contract = contractService.getContractBySessionId(sessionId);
            
            if (contract != null) {
                response.put("success", true);
                response.put("message", "상담 정보를 찾았습니다.");
                response.put("contractId", contract.getContractId());
                response.put("status", contract.getStatus());
                response.put("clientId", contract.getClientId());
                response.put("agentId", contract.getAgentId());
                response.put("sessionId", sessionId);
            } else {
                log.warn("세션 ID에 해당하는 상담 정보가 없습니다: {}", sessionId);
                response.put("success", false);
                response.put("message", "세션 ID에 해당하는 상담을 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("세션 ID로 상담 조회 실패 - 세션 ID: {}, 오류: {}", sessionId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "상담 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테스트용 API - 간단한 응답 확인
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            String currentUserRole = SecurityUtil.getCurrentUserRole();
            
            response.put("success", true);
            response.put("message", "테스트 API 성공");
            response.put("userEmail", currentUserEmail);
            response.put("userRole", currentUserRole);
            response.put("timestamp", new java.util.Date());
            
            log.info("테스트 API 호출 성공 - 사용자: {}, 역할: {}", currentUserEmail, currentUserRole);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("테스트 API 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "테스트 API 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * DB 연결 테스트용 API
     */
    @PostMapping("/test-db")
    public ResponseEntity<Map<String, Object>> testDatabase(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("DB 테스트 시작 - 세션 ID: {}", sessionId);
            
            // 단순히 모든 계약 조회해보기
            List<Contract> contracts = contractService.getAllContracts();
            
            response.put("success", true);
            response.put("message", "DB 연결 성공");
            response.put("contractCount", contracts.size());
            response.put("sessionId", sessionId);
            
            log.info("DB 테스트 성공 - 계약 수: {}", contracts.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("DB 테스트 실패 - 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "DB 테스트 실패: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 