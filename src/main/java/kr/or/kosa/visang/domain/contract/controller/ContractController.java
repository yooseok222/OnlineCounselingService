package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ContractController {

    @Autowired
    private ContractService contractService;

    /**
     * 계약 생성 API
     */
    @PostMapping("/contract")
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody Contract contract) {
        // 디버깅 로그
        System.out.println("계약 생성 요청 수신 (ContractController): " + contract);
        
        // 계약 ID 설정 제거 (데이터베이스에서 자동 생성되도록)
        contract.setContractId(null);
        
        // 상태가 비어있거나 null인 경우 기본값 설정
        if (contract.getStatus() == null || contract.getStatus().trim().isEmpty()) {
            contract.setStatus("완료");
        }
        
        // 실제 저장 시도
        int result = 0;
        try {
            result = contractService.createContract(contract);
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
} 