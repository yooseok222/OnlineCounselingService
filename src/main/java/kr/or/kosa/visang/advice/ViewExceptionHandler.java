package kr.or.kosa.visang.advice;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice(basePackages = "kr.or.kosa.visang.domain.admin.controller")
public class ViewExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException e, Model model) {
        model.addAttribute("message", e.getReason());
        model.addAttribute("code", e.getStatusCode().value());
        return "admin/error/custom-error"; // resources/templates/custom-error.html
    }
}
