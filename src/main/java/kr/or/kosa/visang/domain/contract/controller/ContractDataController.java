package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.service.ContractDataService;
import kr.or.kosa.visang.domain.contract.model.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/contract-data")
public class ContractDataController {

    @Autowired
    private ContractDataService contractDataService;
    
    // 임시 세션 데이터 저장소 (실제로는 데이터베이스나 Redis 등에 저장하는 것이 좋음)
    private static final ConcurrentHashMap<String, Map<String, Object>> sessionDataStore = new ConcurrentHashMap<>();

    /**
     * 모든 계약 목록 조회 API
     */
    @GetMapping("/contracts")
    public ResponseEntity<?> getAllContracts() {
        List<Contract> contracts = contractDataService.getAllContracts();
        Map<String, Object> response = new HashMap<>();
        
        if (contracts != null && !contracts.isEmpty()) {
            response.put("success", true);
            response.put("contracts", contracts);
        } else {
            response.put("success", false);
            response.put("message", "계약 데이터가 없습니다.");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 계약 데이터 조회 API
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<?> getContractData(@PathVariable Long contractId) {
        Map<String, Object> data = contractDataService.getContractData(contractId);
        
        if (data != null && data.containsKey("contract") && data.get("contract") != null) {
            return ResponseEntity.ok(data);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "계약 데이터를 찾을 수 없습니다.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 계약 상태 업데이트 API
     */
    @PutMapping("/contract/{contractId}/status")
    public ResponseEntity<Map<String, Object>> updateContractStatus(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        boolean updated = contractDataService.updateContractStatus(contractId, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", updated);
        response.put("message", updated ? "계약 상태가 업데이트 되었습니다." : "계약 상태 업데이트에 실패했습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 계약 메모 업데이트 API
     */
    @PutMapping("/contract/{contractId}/memo")
    public ResponseEntity<Map<String, Object>> updateContractMemo(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> request) {
        
        String memo = request.get("memo");
        boolean updated = contractDataService.updateContractMemo(contractId, memo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", updated);
        response.put("message", updated ? "계약 메모가 저장되었습니다." : "계약 메모 저장에 실패했습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 계약 생성 API
     */
    @PostMapping("/contract")
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody Contract contract) {
        // 디버깅 로그
        System.out.println("계약 생성 요청 수신: " + contract.toString());
        
        // 계약 ID 설정 제거 (데이터베이스에서 자동 생성되도록)
        contract.setContractId(null);
        
        // 상태가 비어있거나 null인 경우 기본값 설정
        if (contract.getStatus() == null || contract.getStatus().trim().isEmpty()) {
            contract.setStatus("COMPLETED");
        }
        
        // 실제 저장 시도
        int result = 0;
        try {
            result = contractDataService.createContract(contract);
        } catch (Exception e) {
            System.err.println("계약 저장 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("success", result > 0);
        response.put("message", result > 0 ? "계약이 생성되었습니다." : "계약 생성에 실패했습니다.");
        response.put("contractId", contract.getContractId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 세션 데이터 저장 API
     */
    @PostMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> saveSessionData(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> sessionData) {
        
        // 세션 데이터 저장
        sessionDataStore.put(sessionId, sessionData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "세션 데이터가 저장되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 세션 데이터 조회 API
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionData(@PathVariable String sessionId) {
        Map<String, Object> sessionData = sessionDataStore.get(sessionId);
        
        if (sessionData != null) {
            // 세션 데이터를 그대로 반환
            return ResponseEntity.ok(sessionData);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "세션 데이터를 찾을 수 없습니다.");
            return ResponseEntity.ok(response);
        }
    }
} 