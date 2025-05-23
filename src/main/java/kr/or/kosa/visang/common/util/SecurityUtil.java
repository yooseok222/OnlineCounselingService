package kr.or.kosa.visang.common.util;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 관련 유틸리티 클래스
 */
public class SecurityUtil {
    
    /**
     * 현재 로그인된 사용자의 CustomUserDetails를 반환
     * @return CustomUserDetails 또는 null (로그인되지 않은 경우)
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        
        return null;
    }
    
    /**
     * 현재 로그인된 사용자의 이메일을 반환
     * @return 사용자 이메일 또는 null
     */
    public static String getCurrentUserEmail() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
    
    /**
     * 현재 로그인된 사용자의 역할을 반환
     * @return 사용자 역할 또는 null
     */
    public static String getCurrentUserRole() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }
    
    /**
     * 현재 로그인된 사용자의 이름을 반환
     * @return 사용자 이름 또는 null
     */
    public static String getCurrentUserName() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getName() : null;
    }
    
    /**
     * 현재 로그인된 사용자의 ID를 반환
     * @return 사용자 ID 또는 null
     */
    public static Long getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
    
    /**
     * 현재 로그인된 사용자가 상담원인지 확인
     * @return true if agent, false otherwise
     */
    public static boolean isCurrentUserAgent() {
        return "AGENT".equals(getCurrentUserRole());
    }
    
    /**
     * 현재 로그인된 사용자가 고객인지 확인
     * @return true if client, false otherwise
     */
    public static boolean isCurrentUserClient() {
        return "USER".equals(getCurrentUserRole());
    }
} 