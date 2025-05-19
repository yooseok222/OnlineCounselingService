package kr.or.kosa.visang.common.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import kr.or.kosa.visang.common.config.security.exception.EmailNotVerifiedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.io.IOException;

/**
 * 로그인 실패 처리를 위한 커스텀 핸들러
 */
@Slf4j
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        String errorMessage;
        
        // 예외 유형에 따라 다른 메시지 설정
        if (exception instanceof EmailNotVerifiedException || exception instanceof DisabledException ||
            (exception instanceof InternalAuthenticationServiceException && exception.getCause() instanceof EmailNotVerifiedException)) {
            errorMessage = "email-not-verified";
            log.debug("이메일 인증 미완료: {}", exception.getMessage());
        } else if (exception instanceof UsernameNotFoundException) {
            String message = exception.getMessage();
            
            // 이메일 인증 메시지 확인
            if (message != null && message.contains("이메일 인증이 완료되지 않은")) {
                errorMessage = "email-not-verified";
                log.debug("이메일 인증이 완료되지 않은 사용자: {}", exception.getMessage());
            } else {
                errorMessage = "invalid-credentials";
                log.debug("사용자를 찾을 수 없음: {}", exception.getMessage());
            }
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "invalid-credentials";
            log.debug("잘못된 인증 정보: {}", exception.getMessage());
        } else {
            errorMessage = "login-error";
            log.debug("로그인 실패: {}", exception.getMessage());
        }
        
        String email = request.getParameter("email");
        String emailParam = (email != null && !email.isEmpty()) ? ("&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)) : "";
        setDefaultFailureUrl("/login?error=" + errorMessage + emailParam);
        super.onAuthenticationFailure(request, response, exception);
    }
} 