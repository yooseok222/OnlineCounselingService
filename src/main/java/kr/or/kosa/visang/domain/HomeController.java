package kr.or.kosa.visang.domain;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home(HttpServletRequest request, Model model) {
    String currentUri = request.getRequestURI();
    model.addAttribute("currentUri", currentUri);
    return "layout/main";
  }
}
