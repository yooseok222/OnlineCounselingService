package kr.or.kosa.visang.domain.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대코드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    
    /**
     * 초대코드 ID
     */
    private Long invitationId;
    
    /**
     * 초대코드
     */
    private String invitationCode;
    
    /**
     * 회사 ID
     */
    private Long companyId;
    
    /**
     * 회사명
     */
    private String companyName;
    
    /**
     * 관리자 ID
     */
    private Long adminId;
    
    /**
     * 관리자명
     */
    private String adminName;
    
    /**
     * 만료 시간
     */
    private LocalDateTime expiredTime;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 만료 여부
     */
    private boolean expired;
} 