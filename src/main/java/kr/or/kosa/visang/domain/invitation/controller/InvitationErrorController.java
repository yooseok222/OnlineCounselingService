package kr.or.kosa.visang.domain.invitation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class InvitationErrorController {
    
    /**
     * 초대링크 에러 페이지
     * URL: /invitation/error
     */
    @GetMapping("/invitation/error")
    public String showInvitationError(Model model) {
        log.info("초대링크 에러 페이지 요청");
        return "invitation/error";
    }
} 