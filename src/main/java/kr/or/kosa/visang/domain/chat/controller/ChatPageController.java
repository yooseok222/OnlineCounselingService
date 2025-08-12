package kr.or.kosa.visang.domain.chat.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/chat")
public class ChatPageController {

    private final ContractService contractService;

    public ChatPageController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public String chatPage(
            @RequestParam("room") Long roomId,
            Authentication authentication,
            Model model
    ) {
        // 현재 로그인한 사용자 정보
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        Long userId = user.getUserId();

        // contractService에 참여자 여부 요청 -> 참여자만 통과
        if (!contractService.isParticipant(roomId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "해당 채팅방에 접근 권한이 없습니다."
            );
        }

        model.addAttribute("room", roomId);
        return "chat/chat";
    }


}
