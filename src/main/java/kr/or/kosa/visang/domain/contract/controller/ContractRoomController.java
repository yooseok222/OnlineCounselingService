package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ContractRoomController {
    
    @Autowired
    private ContractService contractService;
    
    @GetMapping("/contract")
    public String contractEntry() {
        return "contract/entryPage";
    }

    @GetMapping("/contract/room")
    public String enterContractRoom(@RequestParam(required = false) String role) {
        // 역할에 따라 다른 페이지로 라우팅
        if ("agent".equals(role)) {
            return "contract/agentRoom"; // 상담원 페이지
        } else if ("client".equals(role)) {
            return "contract/clientRoom"; // 고객 페이지
        } else {
            // 역할이 지정되지 않은 경우 입장 페이지로 리다이렉트
            return "redirect:/contract";
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