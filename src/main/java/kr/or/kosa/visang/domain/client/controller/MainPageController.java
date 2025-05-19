package kr.or.kosa.visang.domain.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {
    
    @GetMapping("/main-page")
    public String mainPage() {
        return "mainPage";
    }
} 