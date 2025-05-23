package kr.or.kosa.visang.domain.admin.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.model.UpdateAgentDto;
import kr.or.kosa.visang.domain.agent.service.AgentService;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AgentService agentService;
    private final ContractService contractService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        String uri = request.getRequestURI();
        model.addAttribute("currentUri", uri);

        model.addAttribute("contentFragment", "adminDashboard");
        model.addAttribute("scriptFragment", "adminDashboard");

        return "admin/adminLayout";
    }

    @GetMapping("/list")
    public String list(HttpServletRequest request, Model model) {
        model.addAttribute("agentList", agentService.getAgentList());
            // 공통 레이아웃으로 전체 페이지 렌더링
        model.addAttribute("contentFragment", "adminAgentManagement");
        model.addAttribute("scriptFragment", "adminAgentManagement");

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
    @ResponseBody
    public Agent detail(@PathVariable Long id){
        return agentService.getAgentInfo(id);
    }

    // 에이전트 수정
    @PostMapping("/update/{id}")
    public String updateAgent(@PathVariable Long id,
                              @ModelAttribute UpdateAgentDto agentDto,
                              RedirectAttributes redirectAttributes) {
        try {
            agentService.updateAgent(id, agentDto);
            redirectAttributes.addFlashAttribute("message", "상담사 정보가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 중 오류 발생: " + e.getMessage());
        }

        return "redirect:/admin/list";
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
    @ResponseBody
    public List<Contract> schedule(
            @RequestParam Long id,
            @RequestParam String year,
            @RequestParam String month,
            Model model
    ) {
         return contractService.getMonthlyScheduleByAgentId(id, year, month);
    }
}
