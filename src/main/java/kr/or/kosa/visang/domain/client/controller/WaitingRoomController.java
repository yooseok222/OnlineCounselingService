package kr.or.kosa.visang.domain.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WaitingRoomController {

    /**
     * 대기실 페이지 제공
     * @param contractId 계약 ID
     * @param model 모델 객체
     * @return 대기실 뷰 이름
     */
    @GetMapping("/waiting-room")
    public String waitingRoom(@RequestParam("contractId") Long contractId, Model model) {
        log.info("대기실 진입 - contractId: {}", contractId);
        
        // contractId를 모델에 추가하여 템플릿에서 사용 가능하도록 함
        model.addAttribute("contractId", contractId);
        
        return "client/waitingRoom";
    }
} 