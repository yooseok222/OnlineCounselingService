package kr.or.kosa.visang.common.util;

import java.util.regex.Pattern;

/**
 * 애플리케이션 전체에서 사용되는 정규식 패턴 상수들을 중앙화
 */
public final class RegexPatterns {
    // 클래스 인스턴스화 방지
    private RegexPatterns() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }
    
    // 각 정규식의 문자열 표현 (JavaScript에서 사용 가능하도록)
    public static final String NAME_REGEX_STR = "^[가-힣]{2,10}$";
    public static final String EMAIL_REGEX_STR = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    public static final String PASSWORD_REGEX_STR = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$";
    public static final String PHONE_REGEX_STR = "^(01[016789]{1})(\\d{3,4})(\\d{4})$|^(01[016789]{1})-(\\d{3,4})-(\\d{4})$";
    public static final String SSN_REGEX_STR = "^\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])-?[1-8]\\d{6}$";
    
    // Java Pattern 객체 (백엔드 검증용)
    public static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX_STR);
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX_STR);
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX_STR);
    public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX_STR);
    public static final Pattern SSN_PATTERN = Pattern.compile(SSN_REGEX_STR);
    
    // 오류 메시지
    public static final String NAME_ERROR_MSG = "이름은 2~10자의 한글만 입력 가능합니다.";
    public static final String EMAIL_ERROR_MSG = "유효한 이메일 형식이 아닙니다.";
    public static final String PASSWORD_ERROR_MSG = "비밀번호는 8자 이상이며, 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 각각 하나 이상 포함해야 합니다.";
    public static final String PHONE_ERROR_MSG = "전화번호 형식이 올바르지 않습니다.";
    public static final String SSN_ERROR_MSG = "주민등록번호 형식이 올바르지 않습니다.";
} 