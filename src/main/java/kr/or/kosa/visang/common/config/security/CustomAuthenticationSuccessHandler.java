package kr.or.kosa.visang.common.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * 인증 성공 처리를 위한 커스텀 핸들러
 * 사용자 유형에 따라 다른 대시보드로 리다이렉션
 */
@Slf4j
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // 로그 남기기
        log.info("로그인 성공: 이메일={}, 역할={}", userDetails.getUsername(), userDetails.getRole());
        
        // 역할에 따라 리다이렉션 URL 설정
        String redirectUrl;
        
        switch (userDetails.getRole()) {
            case "USER":
                redirectUrl = "/user/dashboard";
                break;
            case "AGENT":
                redirectUrl = "/agent/dashboard";
                break;
            case "ADMIN":
                redirectUrl = "/admin/dashboard";
                break;
            default:
                redirectUrl = "/";
                break;
        }
        
        // 리다이렉션
        response.sendRedirect(redirectUrl);
    }
} 