package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.ContractDetail;
import kr.or.kosa.visang.domain.contract.model.EndContractMessage;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    
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
            System.err.println("=== 상담방 참여 API 실패 ===");
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류 타입: " + e.getClass().getSimpleName());
            e.printStackTrace();
            
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
            @RequestParam String memo,
            @RequestParam(required = false) String sessionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            String currentUserRole = SecurityUtil.getCurrentUserRole();
            
            if (currentUserEmail == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("상담 종료 요청 - 계약 ID: {}, 메모: {}, 사용자: {}, 역할: {}, 세션 ID: {}", 
                    contractId, memo, currentUserEmail, currentUserRole, sessionId);
            
            // 상담 종료 처리
            int result = contractService.endConsultation(contractId, memo);
            
            if (result > 0) {
                // 상담 종료 성공 시 WebSocket을 통해 모든 참여자에게 알림 전송
                try {
                    EndContractMessage endMessage = new EndContractMessage();
                    endMessage.setMessage("상담이 종료되었습니다. 메인 페이지로 이동합니다.");
                    endMessage.setContractId(contractId);
                    endMessage.setRedirectUrl("/");
                    
                    // 전역 상담 종료 토픽으로 메시지 전송 (모든 사용자에게)
                    messagingTemplate.convertAndSend("/topic/endConsult", endMessage);
                    
                    // 특정 세션 ID가 있는 경우 해당 방에도 메시지 전송
                    if (sessionId != null && !sessionId.trim().isEmpty()) {
                        messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/endConsult", endMessage);
                        log.info("세션별 상담 종료 메시지 전송 완료 - 세션 ID: {}", sessionId);
                    }
                    
                    log.info("상담 종료 WebSocket 메시지 전송 완료 - 계약 ID: {}", contractId);
                } catch (Exception wsException) {
                    log.error("WebSocket 메시지 전송 실패: {}", wsException.getMessage(), wsException);
                    // WebSocket 전송 실패해도 상담 종료는 성공으로 처리
                }
                
                response.put("success", true);
                response.put("message", "상담이 성공적으로 종료되었습니다.");
                response.put("contractId", contractId);
                response.put("redirectUrl", "/");
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
     * 계약 정보 조회 (고객 이메일 포함)
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<Map<String, Object>> getContract(@PathVariable Long contractId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("계약 정보 조회 요청 - 계약 ID: {}", contractId);
            
            // 먼저 기본 계약 정보 조회
            Contract basicContract = contractService.getContractById(contractId);
            log.info("기본 계약 정보 조회 결과: {}", basicContract);
            
            if (basicContract == null) {
                log.warn("기본 계약 정보를 찾을 수 없음 - 계약 ID: {}", contractId);
                response.put("success", false);
                response.put("message", "계약을 찾을 수 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            // 계약 상세 정보 조회 (고객 및 상담원 정보 포함)
            // 먼저 템플릿 포함 조회 시도, 실패하면 기본 조회 사용
            ContractDetail contractDetail = null;
            try {
                contractDetail = contractService.getContractDetail(contractId);
                log.info("계약 상세 정보 조회 결과 (템플릿 포함): {}", contractDetail);
            } catch (Exception e) {
                log.warn("템플릿 포함 조회 실패, 이메일 포함 조회 시도: {}", e.getMessage());
                contractDetail = contractService.getContractDetailWithEmail(contractId);
                log.info("계약 상세 정보 조회 결과 (이메일 포함): {}", contractDetail);
            }
            
            if (contractDetail != null) {
                // 응답 데이터 구성
                Map<String, Object> contractData = new HashMap<>();
                contractData.put("contractId", contractDetail.getContractId());
                contractData.put("status", contractDetail.getStatus());
                contractData.put("createdAt", contractDetail.getCreatedAt());
                contractData.put("contractTime", contractDetail.getContractTime());
                contractData.put("clientId", contractDetail.getClientId());
                contractData.put("agentId", contractDetail.getAgentId());
                contractData.put("memo", contractDetail.getMemo());
                
                // 고객 정보
                contractData.put("clientName", contractDetail.getClientName());
                contractData.put("clientEmail", contractDetail.getClientEmail());
                contractData.put("clientPhoneNumber", contractDetail.getClientPhoneNumber());
                contractData.put("clientAddress", contractDetail.getClientAddress());
                
                // 상담원 정보
                contractData.put("agentName", contractDetail.getAgentName());
                contractData.put("agentEmail", contractDetail.getAgentEmail());
                contractData.put("agentPhoneNumber", contractDetail.getAgentPhoneNumber());
                contractData.put("agentAddress", contractDetail.getAgentAddress());
                
                response.put("success", true);
                response.put("contract", contractData);
                
                log.info("계약 정보 조회 성공 - 계약 ID: {}, 고객 이메일: {}", contractId, contractDetail.getClientEmail());
            } else {
                // 상세 정보 조회 실패 시 기본 계약 정보로 대체
                log.warn("계약 상세 정보 조회 실패, 기본 정보로 대체 - 계약 ID: {}", contractId);
                
                // 기본 계약 정보로 응답 구성
                Map<String, Object> contractData = new HashMap<>();
                contractData.put("contractId", basicContract.getContractId());
                contractData.put("status", basicContract.getStatus());
                contractData.put("createdAt", basicContract.getCreatedAt());
                contractData.put("contractTime", basicContract.getContractTime());
                contractData.put("clientId", basicContract.getClientId());
                contractData.put("agentId", basicContract.getAgentId());
                contractData.put("memo", basicContract.getMemo());
                
                // 고객 이메일 별도 조회
                String clientEmail = contractService.getClientEmailByContractId(contractId);
                if (clientEmail == null || clientEmail.isEmpty()) {
                    clientEmail = "customer@example.com"; // 기본값
                    log.warn("고객 이메일 조회 실패, 기본값 사용 - 계약 ID: {}", contractId);
                }
                contractData.put("clientEmail", clientEmail);
                
                response.put("success", true);
                response.put("contract", contractData);
                response.put("warning", "상세 정보 조회 실패로 기본 정보만 제공됩니다.");
                
                log.info("기본 계약 정보로 응답 구성 완료 - 계약 ID: {}", contractId);
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
     * 세션 ID로 계약 ID 조회
     */
    /* 
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getContractBySessionId(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("세션 ID로 계약 ID 조회 요청: {}", sessionId);
            
            // 세션 ID로 계약 조회
            Contract contract = contractService.getContractBySessionId(sessionId);
            
            if (contract != null) {
                response.put("success", true);
                response.put("contractId", contract.getContractId());
                response.put("status", contract.getStatus());
                response.put("message", "계약 정보 조회 성공");
                
                log.info("세션 ID {}로 계약 ID {} 조회 성공", sessionId, contract.getContractId());
            } else {
                response.put("success", false);
                response.put("message", "해당 세션의 계약 정보를 찾을 수 없습니다.");
                
                log.warn("세션 ID {}에 해당하는 계약을 찾을 수 없습니다", sessionId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("세션 ID로 계약 ID 조회 실패: sessionId={}", sessionId, e);
            response.put("success", false);
            response.put("message", "계약 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    */
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
    
    /**
     * DB 연결 테스트용 API
     */
    @GetMapping("/db-test")
    public ResponseEntity<Map<String, Object>> dbTest() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Contract> contracts = contractService.getAllContracts();
            
            response.put("success", true);
            response.put("message", "DB 연결 테스트 성공");
            response.put("contractCount", contracts != null ? contracts.size() : 0);
            response.put("timestamp", new java.util.Date());
            
            log.info("DB 연결 테스트 성공 - 계약 수: {}", contracts != null ? contracts.size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("DB 연결 테스트 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "DB 연결 테스트 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 현재 로그인한 사용자 정보 확인 API
     */
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            String currentUserRole = SecurityUtil.getCurrentUserRole();
            kr.or.kosa.visang.common.config.security.CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
            
            response.put("success", true);
            response.put("message", "사용자 정보 조회 성공");
            response.put("userEmail", currentUserEmail);
            response.put("userRole", currentUserRole);
            response.put("timestamp", new java.util.Date());
            
            if (currentUser != null) {
                response.put("userId", currentUser.getUserId());
                response.put("userName", currentUser.getName());
                response.put("clientId", currentUser.getClientId());
                response.put("agentId", currentUser.getAgentId());
                response.put("companyId", currentUser.getCompanyId());
            }
            
            log.info("사용자 정보 조회 성공 - 이메일: {}, 역할: {}", currentUserEmail, currentUserRole);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "사용자 정보 조회 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 디버깅용 API - 모든 계약 조회 및 세션 ID 확인
     */
    @GetMapping("/debug/contracts")
    public ResponseEntity<Map<String, Object>> debugContracts() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Contract> allContracts = contractService.getAllContracts();
            
            response.put("success", true);
            response.put("totalContracts", allContracts.size());
            response.put("contracts", allContracts);
            
            // 세션 ID가 포함된 계약들만 필터링
            List<Contract> contractsWithSessionId = allContracts.stream()
                .filter(c -> c.getMemo() != null && c.getMemo().contains("SessionId:"))
                .toList();
            
            response.put("contractsWithSessionId", contractsWithSessionId);
            response.put("sessionIdContractCount", contractsWithSessionId.size());
            
            log.info("디버깅 - 전체 계약 수: {}, 세션 ID 포함 계약 수: {}", 
                    allContracts.size(), contractsWithSessionId.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("디버깅 API 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "디버깅 API 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 디버깅용 API - 특정 계약 ID 상세 정보
     */
    @GetMapping("/debug/contract/{contractId}")
    public ResponseEntity<Map<String, Object>> debugContract(@PathVariable Long contractId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("디버깅 - 계약 ID {} 상세 조회", contractId);
            
            // 1. 기본 계약 정보 조회
            Contract basicContract = contractService.getContractById(contractId);
            response.put("basicContract", basicContract);
            
            // 2. 계약 상세 정보 조회
            ContractDetail contractDetail = contractService.getContractDetail(contractId);
            response.put("contractDetail", contractDetail);
            
            // 3. 고객 이메일 조회
            String clientEmail = contractService.getClientEmailByContractId(contractId);
            response.put("clientEmail", clientEmail);
            
            // 4. 모든 계약 목록 (최근 10개)
            List<Contract> allContracts = contractService.getAllContracts();
            List<Contract> recentContracts = allContracts.stream()
                .limit(10)
                .toList();
            response.put("recentContracts", recentContracts);
            
            response.put("success", true);
            response.put("message", "디버깅 정보 조회 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("디버깅 API 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "디버깅 API 오류: " + e.getMessage());
            response.put("error", e.toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 디버깅용 API - 특정 세션 ID 검색
     */
    @GetMapping("/debug/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> debugSessionSearch(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("디버깅 - 세션 ID 검색: {}", sessionId);
            
            // 모든 계약 조회
            List<Contract> allContracts = contractService.getAllContracts();
            
            // 세션 ID가 포함된 계약 찾기
            List<Contract> matchingContracts = allContracts.stream()
                .filter(c -> c.getMemo() != null && c.getMemo().contains(sessionId))
                .toList();
            
            // 정확한 패턴 매칭
            List<Contract> exactMatches = allContracts.stream()
                .filter(c -> c.getMemo() != null && c.getMemo().contains("SessionId: " + sessionId))
                .toList();
            
            response.put("success", true);
            response.put("searchSessionId", sessionId);
            response.put("totalContracts", allContracts.size());
            response.put("partialMatches", matchingContracts);
            response.put("exactMatches", exactMatches);
            response.put("partialMatchCount", matchingContracts.size());
            response.put("exactMatchCount", exactMatches.size());
            
            // 모든 계약의 메모 내용 출력 (디버깅용)
            List<Map<String, Object>> contractMemos = allContracts.stream()
                .map(c -> {
                    Map<String, Object> memo = new HashMap<>();
                    memo.put("contractId", c.getContractId());
                    memo.put("memo", c.getMemo());
                    memo.put("status", c.getStatus());
                    return memo;
                })
                .toList();
            
            response.put("allContractMemos", contractMemos);
            
            log.info("디버깅 결과 - 부분 매칭: {}, 정확 매칭: {}", 
                    matchingContracts.size(), exactMatches.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("디버깅 세션 검색 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "디버깅 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 