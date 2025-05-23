package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contract.model.ContractStatusCountsByMonthDTO;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final ContractService contractService;

    @RequestMapping("contract-status/month")
    @ResponseBody
    public ContractStatusCountsByMonthDTO getContractStatusByMonth(@AuthenticationPrincipal CustomUserDetails admin,
                                                                   @RequestParam int year,
                                                                   @RequestParam int month) {
        Long companyId = admin.getCompanyId();
        ContractStatusCountsByMonthDTO cont =  contractService.getMonthlyStatusCounts(companyId, year, month);
        System.out.println(cont);
        return cont;
    }
}
