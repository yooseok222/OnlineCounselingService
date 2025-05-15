package kr.or.kosa.visang.domain.counselor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CounselorController {
	
	@GetMapping("/counselor/dashboard")
	public String dashboard(Model model) {
		return "layout/main";

	}
}
