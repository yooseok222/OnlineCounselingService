package kr.or.kosa.visang.domain.agent.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.or.kosa.visang.domain.agent.service.AgentService;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.contract.model.Schedule;

@Controller
@RequestMapping("/agent")
public class AgentController {

	private final AgentService agentService;
	private static final Logger log = LoggerFactory.getLogger(AgentController.class);

	@Autowired
	public AgentController(AgentService agentService) {
		this.agentService = agentService;
	}

	@GetMapping("/dashboard")
	public String dashboard() {
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

	// 스케줄 삭제
	@DeleteMapping("/schedule/delete")
	@ResponseBody
	public ResponseEntity<Map<String, String>> deleteSchedule(@RequestParam("contractId") Long contractId) {
		try {
			agentService.deleteSchedule(contractId);
			return ResponseEntity.ok(Map.of("status", "deleted"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 에러"));
		}
	}

	// 오늘의 계약 조회
	@GetMapping("/today-contracts")
	@ResponseBody
	public List<Schedule> todayContracts(@RequestParam("agentId") Long agentId, @RequestParam("date") String date) {
		return agentService.getTodayContracts(agentId, date);
	}

}
