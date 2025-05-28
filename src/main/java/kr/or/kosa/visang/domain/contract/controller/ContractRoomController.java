package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contract.service.SessionContractMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ContractRoomController {
    
    private final ContractService contractService;
    private final SessionContractMappingService mappingService;
    
    @GetMapping("/contract")
    public String contractEntry() {
        return "contract/entryPage";
    }

    @GetMapping("/contract/room")
    public String enterContractRoom(
            @RequestParam Long contractId,
            @RequestParam String role,
            @RequestParam(required = false) String session,
            HttpSession httpSession,
            Model model) {
        
        // 1. contractId 유효성 검사
        try {
            Contract contract = contractService.getContractById(contractId);
            if (contract == null) {
                log.warn("존재하지 않는 contractId로 접근 시도: {}", contractId);
                model.addAttribute("errorMessage", "존재하지 않는 계약입니다. (ID: " + contractId + ")");
                return "error/contract-not-found";
            }
            
            // 계약 상태 확인
            if (contract.getStatus() != null && "CANCELLED".equals(contract.getStatus())) {
                log.warn("취소된 계약으로 접근 시도: contractId={}", contractId);
                model.addAttribute("errorMessage", "취소된 계약입니다.");
                return "error/contract-not-found";
            }
            
            log.info("계약 검증 성공: contractId={}, status={}", contractId, contract.getStatus());
            
        } catch (Exception e) {
            log.error("계약 정보 조회 중 오류 발생: contractId={}", contractId, e);
            model.addAttribute("errorMessage", "계약 정보를 확인할 수 없습니다: " + e.getMessage());
            return "error/contract-error";
        }
        
        // 2. 세션 ID 처리
        String sessionId = session;
        if (sessionId == null || sessionId.trim().isEmpty()) {
            // 세션 ID가 없으면 새로 생성
            sessionId = "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("새 세션 ID 생성: {}", sessionId);
        } else {
            log.info("기존 세션 ID 사용: {}", sessionId);
        }
        
        // 3. 역할 검증
        if (!"agent".equals(role) && !"client".equals(role)) {
            log.warn("잘못된 역할로 접근 시도: role={}, contractId={}", role, contractId);
            model.addAttribute("errorMessage", "잘못된 역할입니다: " + role);
            return "error/contract-error";
        }
        
        // 4. 세션에서 entryType 정보 가져오기 (고객인 경우)
        String entryType = null;
        String stampImage = null;
        String signatureImage = null;
        
        if ("client".equals(role)) {
            entryType = (String) httpSession.getAttribute("entryType");
            stampImage = (String) httpSession.getAttribute("stampImage");
            signatureImage = (String) httpSession.getAttribute("signatureImage");
            
            log.info("세션에서 가져온 데이터:");
            log.info("- entryType: {}", entryType);
            log.info("- stampImage: {}", stampImage != null ? "존재(" + stampImage.length() + " chars)" : "null");
            log.info("- signatureImage: {}", signatureImage != null ? "존재(" + signatureImage.length() + " chars)" : "null");
        }
        
        // 5. 세션-계약 매핑 생성
        try {
            mappingService.createMapping(sessionId, contractId, role);
            log.info("세션-계약 매핑 생성 완료: session={}, contract={}, role={}", sessionId, contractId, role);
            
        } catch (Exception e) {
            log.error("세션 매핑 생성 실패: session={}, contract={}, role={}", sessionId, contractId, role, e);
            model.addAttribute("errorMessage", "세션 생성에 실패했습니다. 다시 시도해주세요.");
            return "error/contract-error";
        }
        
        // 6. 모델에 필요한 데이터 추가
        model.addAttribute("contractId", contractId);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("role", role);
        
        // 고객인 경우 entryType 정보도 추가
        if ("client".equals(role) && entryType != null) {
            model.addAttribute("entryType", entryType);
            log.info("모델에 entryType 추가: {}", entryType);
            
            // 도장/서명 이미지 데이터도 추가
            if ("stamp".equals(entryType) && stampImage != null) {
                model.addAttribute("stampImage", stampImage);
                log.info("모델에 stampImage 추가 (길이: {} chars)", stampImage.length());
            } else if ("signature".equals(entryType) && signatureImage != null) {
                model.addAttribute("signatureImage", signatureImage);
                log.info("모델에 signatureImage 추가 (길이: {} chars)", signatureImage.length());
            }
        }
        
        // 7. 역할에 따라 적절한 페이지로 라우팅
        if ("agent".equals(role)) {
            log.info("상담원 방 입장: contractId={}, sessionId={}", contractId, sessionId);
            return "contract/agentRoom";
        } else {
            log.info("고객 방 입장: contractId={}, sessionId={}, entryType={}", contractId, sessionId, entryType);
            return "contract/clientRoom";
        }
    }
    
    /**
     * 계약 완료 화면
     */
    @GetMapping("/contract/complete")
    public String contractComplete(Model model) {
        // 모든 계약 목록을 가져와서 모델에 추가
        List<Contract> contracts = contractService.getAllContracts();
        model.addAttribute("contracts", contracts);
        return "contract/contractComplete";
    }
    
    /**
     * 계약 상세 화면
     */
    @GetMapping("/contract/{contractId}")
    public String contractDetail(@PathVariable Long contractId, Model model) {
        Contract contract = contractService.getContractById(contractId);
        model.addAttribute("contract", contract);
        return "contract/contractDetail";
    }
} 