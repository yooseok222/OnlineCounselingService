package kr.or.kosa.visang.config;

import kr.or.kosa.visang.common.util.RegexPatterns;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * 모든 컨트롤러에 전역적으로 적용되는 어드바이스
 * 정규식 패턴을 모든 Thymeleaf 템플릿에 제공
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * 정규식 패턴을 모든 템플릿에 제공
     * Thymeleaf에서 ${regexPatterns.NAME_REGEX_STR} 등으로 접근 가능
     */
    @ModelAttribute("regexPatterns")
    public Map<String, String> regexPatterns() {
        Map<String, String> patterns = new HashMap<>();

        // 정규식 패턴 문자열
        patterns.put("NAME_REGEX_STR", RegexPatterns.NAME_REGEX_STR);
        patterns.put("EMAIL_REGEX_STR", RegexPatterns.EMAIL_REGEX_STR);
        patterns.put("PASSWORD_REGEX_STR", RegexPatterns.PASSWORD_REGEX_STR);
        patterns.put("PHONE_REGEX_STR", RegexPatterns.PHONE_REGEX_STR);
        patterns.put("SSN_REGEX_STR", RegexPatterns.SSN_REGEX_STR);

        // 오류 메시지
        patterns.put("NAME_ERROR_MSG", RegexPatterns.NAME_ERROR_MSG);
        patterns.put("EMAIL_ERROR_MSG", RegexPatterns.EMAIL_ERROR_MSG);
        patterns.put("PASSWORD_ERROR_MSG", RegexPatterns.PASSWORD_ERROR_MSG);
        patterns.put("PHONE_ERROR_MSG", RegexPatterns.PHONE_ERROR_MSG);
        patterns.put("SSN_ERROR_MSG", RegexPatterns.SSN_ERROR_MSG);

        return patterns;
    }
} 