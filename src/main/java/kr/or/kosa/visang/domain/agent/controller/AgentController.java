package kr.or.kosa.visang.domain.agent.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.agent.service.AgentService;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.Page;
import kr.or.kosa.visang.domain.contract.model.Schedule;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final ContractService contractService;
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    public AgentController(AgentService agentService, ContractService contractService) {
        this.agentService = agentService;
        this.contractService = contractService;
    }


    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user,
                            Model model) {

        Long companyId = user.getCompanyId();
        model.addAttribute("companyId", companyId);
        model.addAttribute("templates", agentService.findByCompanyId(companyId));


        if ("AGENT".equals(user.getRole())) {
            model.addAttribute("agentId", user.getAgentId());
            model.addAttribute("contentFragment", "agent/agentDashboard");
            model.addAttribute("scriptFragment", "agent/agentDashboard");
            model.addAttribute("sideFragment", "agent/agentSidebar");
        }
        return "layout/main";
    }

    // 고객 이메일 조회
    @GetMapping("/client/search")
    @ResponseBody
    public ResponseEntity<Client> getClientByEmail(@RequestParam("email") String email) {
        System.out.println("[이메일 검색] email: " + email);
        Client client = agentService.findByEmail(email);
        if (client != null) {
            return ResponseEntity.ok(client);
        } else {
            System.out.println("고객 없음");
            return ResponseEntity.notFound().build();
        }
    }

    // 스케줄 + 계약 + 초대코드 생성
    @PostMapping("/schedule/add")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addSchedule(@RequestBody Schedule dto) {
        try {
            String code = agentService.addSchedule(dto);
            Map<String, String> body = Map.of("invitationCode", code);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 에러"));
        }
    }

    // 상담원(개인별) 전체 일정 조회
    @GetMapping("/schedule/list")
    public ResponseEntity<List<Schedule>> listSchedules(@RequestParam("agentId") Long agentId) {
        List<Schedule> schedules = agentService.getSchedules(agentId);
        return ResponseEntity.ok(schedules);
    }


    // 같은 고객 & 입력한 시간 => 이미 일정 있는지 확인
    @GetMapping("/schedule/check")
    @ResponseBody
    public Map<String, Boolean> checkClientSchedule(@RequestParam("clientId") Long clientId,
                                                    @RequestParam("contractTime") String contractTime) {
        boolean exists = agentService.isScheduleExists(clientId, contractTime);
        return Map.of("exists", exists);
    }

    // 같은 상담사 & 입력한 시간 => 이미 잡힌 일정 확인
    @GetMapping("/schedule/checkAgent")
    @ResponseBody
    public Map<String, Boolean> checkAgentSchedule(@RequestParam("agentId") Long agentId,
                                                   @RequestParam("contractTime") String contractTime) {
        boolean exists = agentService.isAgentScheduleExists(agentId, contractTime);
        return Map.of("exists", exists);
    }

    // 고객 & 상담사 중복 검사 => 수정
    @PutMapping("/schedule/update")
    @ResponseBody
    public ResponseEntity<?> updateSchedule(@RequestBody Schedule dto) {
        try {
            agentService.updateSchedule(dto);
            return ResponseEntity.ok(Map.of("result", "updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 에러"));
        }
    }

    // 오늘의 계약 조회
    @GetMapping("/today-contracts")
    @ResponseBody
    public List<Schedule> todayContracts(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("date") String date) {
        Long agentId = userDetails.getAgentId();
        return agentService.getTodayContracts(agentId, date);
    }

    //계약 상태에 따라 count
    @GetMapping("/contract-status-counts")
    @ResponseBody
    public Map<String, Integer> getContractStatusCounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long agentId = userDetails.getAgentId();
        return agentService.getContractStatusCounts(agentId);
    }

    // 계약 상태에 따라 list 페이징
    @GetMapping("/contracts-by-status")
    @ResponseBody
    public Map<String, Object> getContractsByStatusPaged(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("status") String status,
            @RequestParam(value = "sort", defaultValue = "DESC") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Long agentId = userDetails.getAgentId();
        Page<Contract> contractPage = agentService.getContractsByStatusPaged(agentId, status, sort, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("contracts", contractPage.getContent());
        result.put("totalCount", contractPage.getTotal());
        result.put("currentPage", contractPage.getCurrentPage());
        result.put("totalPages", contractPage.getTotalPages());
        return result;
    }

    // 로그인된 상담사의 companyId로 계약서 템플릿 목록 가져오기
    @GetMapping("/contract-templates")
    @ResponseBody
    public List<ContractTemplate> getContractTemplates(@AuthenticationPrincipal CustomUserDetails user) {
        return agentService.findByCompanyId(user.getCompanyId());
    }

    //통화시작 '진행중' 업데이트
    @PutMapping("/schedule/status/{contractId}")
    public ResponseEntity<Map<String, Boolean>> updateStatus(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("updated", false));
        }

        contractService.updateCallContractStatus(contractId, newStatus);
        return ResponseEntity.ok(Map.of("updated", true));
    }


}
