package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contract.model.ContractCompleteCountsByMonthDTO;
import kr.or.kosa.visang.domain.contract.model.ContractStatusCountsByMonthDTO;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final ContractService contractService;

    @GetMapping("contract-status/month")
    @ResponseBody
    public ContractStatusCountsByMonthDTO getContractStatusByMonth(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam int year,
            @RequestParam int month) {
        Long companyId = admin.getCompanyId();
        return contractService.getMonthlyStatusCounts(companyId, year, month);
    }

    @GetMapping("/contract/completed/month")
    @ResponseBody
    public ContractCompleteCountsByMonthDTO apiMonthlyCompleted(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long companyId = admin.getCompanyId();
        System.out.println("year = " + year);
        System.out.println("month = " + month);

        ContractCompleteCountsByMonthDTO con = contractService.getLastFiveMonthsCompleted(companyId, year, month);
        System.out.println("con = " + con);
        return con;
    }
}
