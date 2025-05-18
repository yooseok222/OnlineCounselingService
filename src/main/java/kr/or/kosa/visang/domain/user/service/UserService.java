package kr.or.kosa.visang.domain.user.service;

import kr.or.kosa.visang.domain.user.dto.UserRegistrationRequest;
import kr.or.kosa.visang.domain.user.dto.UserResponse;

/**
 * 사용자 관련 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * 회원가입
     * @param request 회원가입 요청 정보
     * @return 생성된 사용자 정보
     */
    UserResponse register(UserRegistrationRequest request);
    
    /**
     * 이메일 중복 확인
     * @param email 확인할 이메일
     * @return 중복 여부 (true: 중복, false: 중복아님)
     */
    boolean isEmailDuplicated(String email);
    
    /**
     * 전화번호 중복 확인
     * @param phoneNumber 확인할 전화번호
     * @return 중복 여부 (true: 중복, false: 중복아님)
     */
    boolean isPhoneNumberDuplicated(String phoneNumber);
    
    /**
     * 주민번호 중복 확인
     * @param ssn 확인할 주민번호
     * @return 중복 여부 (true: 중복, false: 중복아님)
     */
    boolean isSsnDuplicated(String ssn);
    
    /**
     * 이메일 인증 요청
     * @param email 인증할 이메일
     * @return 인증 성공 여부
     */
    boolean requestEmailVerification(String email);
    
    /**
     * 이메일 인증 확인
     * @param email 인증할 이메일
     * @param verificationCode 인증 코드
     * @return 인증 성공 여부
     */
    boolean verifyEmail(String email, String verificationCode);
} 