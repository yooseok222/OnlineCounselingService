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
} 