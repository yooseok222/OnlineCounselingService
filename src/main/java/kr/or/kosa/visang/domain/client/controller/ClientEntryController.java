package kr.or.kosa.visang.domain.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientEntryController {

    /**
     * 고객 입장 페이지 제공
     * @return 고객 입장 뷰 이름
     */
    @GetMapping("/client-entry")
    public String clientEntry() {
        return "client/clientEntry";
    }
} 