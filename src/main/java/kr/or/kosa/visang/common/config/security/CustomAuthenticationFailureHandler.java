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
        
        log.debug("인증 실패: 예외 타입={}, 메시지={}", exception.getClass().getName(), exception.getMessage());
        
        String errorMessage;
        String email = request.getParameter("email");
        
        // 이메일 인증 예외 처리 강화
        if (exception instanceof EmailNotVerifiedException) {
            log.info("이메일 인증 미완료 예외 발생: {}", email);
            errorMessage = "email-not-verified";
        } 
        // 내부 예외가 EmailNotVerifiedException인 경우도 확인
        else if (exception instanceof InternalAuthenticationServiceException && 
                 exception.getCause() instanceof EmailNotVerifiedException) {
            log.info("내부 예외로 인한 이메일 인증 미완료: {}", email);
            errorMessage = "email-not-verified";
        }
        // DisabledException도 이메일 인증 미완료로 처리 (계정 비활성화)
        else if (exception instanceof DisabledException) {
            log.info("비활성화된 계정으로 인한 이메일 인증 미완료: {}", email);
            errorMessage = "email-not-verified";
        } 
        // UsernameNotFoundException 메시지 확인
        else if (exception instanceof UsernameNotFoundException) {
            String message = exception.getMessage();
            
            if (message != null && message.contains("이메일 인증이 완료되지 않은")) {
                log.info("사용자 조회 실패 - 이메일 인증 미완료: {}", email);
                errorMessage = "email-not-verified";
            } else {
                log.info("사용자 조회 실패: {}", email);
                errorMessage = "invalid-credentials";
            }
        } 
        // 잘못된 인증 정보
        else if (exception instanceof BadCredentialsException) {
            log.info("잘못된 인증 정보: {}", email);
            errorMessage = "invalid-credentials";
        } 
        // 기타 인증 오류
        else {
            log.info("기타 로그인 실패: {}. 예외: {}", email, exception.getMessage());
            errorMessage = "login-error";
        }
        
        // 이메일 파라미터 추가하여 로그인 페이지로 리다이렉트
        String emailParam = (email != null && !email.isEmpty()) ? 
                            ("&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)) : "";
        
        String redirectUrl = "/login?error=" + errorMessage + emailParam;
        log.debug("로그인 실패 리다이렉트: {}", redirectUrl);
        
        setDefaultFailureUrl(redirectUrl);
        super.onAuthenticationFailure(request, response, exception);
    }
} 