package kr.or.kosa.visang.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 이름
     */
    private String name;
    
    /**
     * 이메일
     */
    private String email;
    
    /**
     * 전화번호
     */
    private String phoneNumber;
    
    /**
     * 주소
     */
    private String address;
    
    /**
     * 역할
     */
    private String role;
    
    /**
     * 생성일시
     */
    private LocalDateTime createdAt;
    
    /**
     * 회사 ID (상담원, 관리자만 해당)
     */
    private Long companyId;
    
    /**
     * 회사명 (상담원, 관리자만 해당)
     */
    private String companyName;
    
    /**
     * 상태 (상담원만 해당)
     */
    private String state;
    
    /**
     * 프로필 이미지 URL (고객, 상담원만 해당)
     */
    private String profileImageUrl;
} 