package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contract.model.ContractCompleteCountsByMonthDTO;
import kr.or.kosa.visang.domain.contract.model.ContractStatusCountsByMonthDTO;
import kr.or.kosa.visang.domain.contract.model.RecentCompletedContract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

        return contractService.getLastFiveMonthsCompleted(companyId, year, month);
    }

    @GetMapping("/contract/recent-completed")
    @ResponseBody
    public List<RecentCompletedContract> getRecentCompleted(
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        System.out.println("getRecentCompleted() called");
        Long companyId = admin.getCompanyId();
        return contractService.getRecentCompletedContract(companyId);
    }

    @GetMapping("/contract/in-progress")
    @ResponseBody
    public List<RecentCompletedContract> getInProgress(
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        Long companyId = admin.getCompanyId();
        return contractService.getRecentInProgressContract(companyId);
    }
}
