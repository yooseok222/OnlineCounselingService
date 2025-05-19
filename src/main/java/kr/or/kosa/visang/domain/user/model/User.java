package kr.or.kosa.visang.domain.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 사용자 기본 모델 클래스
 * 모든 사용자 유형(고객, 상담원, 관리자)의 공통 속성을 정의
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;             // 사용자 ID
    private String name;         // 이름
    private String email;        // 이메일
    private String password;     // 비밀번호 (암호화됨)
    private String phoneNumber;  // 전화번호
    private String address;      // 주소
    private String role;         // 역할 (USER, AGENT, ADMIN)
    private boolean emailVerified; // 이메일 인증 여부
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
    private String profileImageUrl; // 프로필 이미지 URL
    
    /**
     * 유효성 검증
     */
    public User validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수 입력 항목입니다.");
        }
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수 입력 항목입니다.");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수 입력 항목입니다.");
        }
        
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수 입력 항목입니다.");
        }
        
        return this;
    }

    /**
     * 이메일 형식 유효성 검증
     */
    public User validateEmail() {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (email == null || !email.matches(emailRegex)) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        }
        return this;
    }

    /**
     * 비밀번호 형식 유효성 검증
     */
    public User validatePassword() {
        String pwdRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$";
        if (password == null || !password.matches(pwdRegex)) {
            throw new IllegalArgumentException("비밀번호는 8~20자의 영문 대소문자, 숫자, 특수문자(!@#$%^&*)를 포함해야 합니다.");
        }
        return this;
    }

    /**
     * 전화번호 형식 유효성 검증
     */
    public User validatePhoneNumber() {
        String phoneRegex = "^(01[016789]-?\\d{3,4}-?\\d{4})$";
        if (phoneNumber == null || !phoneNumber.matches(phoneRegex)) {
            throw new IllegalArgumentException("유효하지 않은 전화번호 형식입니다.");
        }
        return this;
    }
} 