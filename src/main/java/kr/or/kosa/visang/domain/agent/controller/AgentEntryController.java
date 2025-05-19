package kr.or.kosa.visang.domain.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AgentEntryController {

    /**
     * 상담원 입장 페이지 제공
     * @return 상담원 입장 뷰 이름
     */
    @GetMapping("/agent-entry")
    public String agentEntry() {
        return "agent/agentEntry";
    }
} 