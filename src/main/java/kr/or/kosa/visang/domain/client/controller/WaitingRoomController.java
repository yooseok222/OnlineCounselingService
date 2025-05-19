package kr.or.kosa.visang.domain.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WaitingRoomController {

    /**
     * 대기실 페이지 제공
     * @return 대기실 뷰 이름
     */
    @GetMapping("/waiting-room")
    public String waitingRoom() {
        return "client/waitingRoom";
    }
} 