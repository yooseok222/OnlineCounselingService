package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.service.AgentService;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AgentService agentService;
    private final ContractService contractService;

    @GetMapping("/list")
    public String list(@RequestParam(required=false) boolean ajax, Model model) {
        model.addAttribute("agentList", agentService.getAgentList());
            // 공통 레이아웃으로 전체 페이지 렌더링
        model.addAttribute("contentFragment", "admin/adminAgentManagement :: content");
        model.addAttribute("scriptFragment", "admin/adminAgentManagement :: script");
        return "admin/adminLayout";

    }


    @GetMapping(value="/search")
    @ResponseBody
    public List<Agent> searchAjax(
            @RequestParam(required=false) String name,
            @RequestParam(required=false) String email,
            @RequestParam(required=false) String state
    ) {
        return agentService.searchAgent(name, email, state);
    }

    // 상세정보 조회
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("agent", agentService.getAgentInfo(id));
        return "admin/adminDetail";
    }

    // 에이전트 수정
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("agentDto", agentService.getAgentInfo(id));
        return "admin/adminEdit";
    }

    // 에이전트 상태 변경
    @PostMapping("/state/{id}")
    public String status(@PathVariable Long id, @RequestParam String state) {
        agentService.updateAgentStatus(id, state);
        return "redirect:/admin/list";
    }

    // 에이전트 비밀번호 변경
    @PostMapping("/password/{id}")
    public String password(@PathVariable Long id, @RequestParam String password) {
        agentService.updateAgentPassword(id, password);
        return "redirect:/admin/list";
    }


    @GetMapping("/schedule")
    public String schedule(
            @RequestParam Long id,
            @RequestParam String year,
            @RequestParam String month,
            Model model
    ) {
        List<Contract> schedule = contractService.getMonthlyScheduleByAgentId(id, year, month);
        model.addAttribute("schedule", schedule);
        return "admin/adminSchedule";
    }
}
