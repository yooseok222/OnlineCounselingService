package kr.or.kosa.visang.domain.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AgentEntryController {

    /**
     * 상담원 입장 페이지 제공
     * URL: /agent-entry?contractId=xxx&session=yyy
     * @return 상담원 입장 뷰 이름
     */
    @GetMapping("/agent-entry")
    public String agentEntry(
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) String session,
            Model model) {
        
        log.info("상담원 입장 페이지 접근 - contractId: {}, session: {}", contractId, session);
        
        // 계약 ID와 세션 ID를 템플릿에 전달
        if (contractId != null) {
            model.addAttribute("hasContract", true);
            model.addAttribute("contractId", contractId);
        } else {
            model.addAttribute("hasContract", false);
        }
        
        if (session != null) {
            model.addAttribute("sessionId", session);
        }
        
        return "agent/agentEntry";
    }
} 