package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.domain.contract.enums.ContractStatus;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContractController {
    private final ContractService contractService;

    @GetMapping("/contract-list/{status}")
    //public String templateList(@AuthenticationPrincipal CustomUserDetails admin,Model model) {
    public String templateList(Model model, @PathVariable("status") String status) {
        //Long companyId = admin.getCompanyId(); // 로그인한 사용자의 회사 ID
        Long id = 1L; // 예시로 회사 ID를 1로 설정, 실제로는 로그인한 사용자의 회사 ID를 사용해야 함

        try {
            String enumName = status.replace("-", "_").toUpperCase();
            ContractStatus contractStatus = ContractStatus.valueOf(enumName);
            System.out.println("contractStatus = " + contractStatus);
            model.addAttribute("contractList", contractService.getContractByStatus(id, contractStatus.name()));
            model.addAttribute("pageTitle", contractStatus.name().replace("_", " "));

            // 공통 레이아웃으로 전체 페이지 렌더링
            model.addAttribute("contentFragment", "contractManagement");
            model.addAttribute("scriptFragment", "contractManagement");
            return "admin/adminLayout";
        } catch (IllegalArgumentException e) {
            // 잘못된 status가 들어왔을 때
            return "error/404";
        }
    }
}
