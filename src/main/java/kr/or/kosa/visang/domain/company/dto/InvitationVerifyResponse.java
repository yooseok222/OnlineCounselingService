package kr.or.kosa.visang.domain.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 초대코드 검증 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationVerifyResponse {
    
    /**
     * 검증 결과
     */
    private boolean valid;
    
    /**
     * 회사 ID
     */
    private Long companyId;
    
    /**
     * 회사명
     */
    private String companyName;
    
    /**
     * 관리자명
     */
    private String adminName;
    
    /**
     * 오류 메시지 (검증 실패 시)
     */
    private String errorMessage;
    
    /**
     * 성공 응답 생성
     * @param companyId 회사 ID
     * @param companyName 회사명
     * @param adminName 관리자명
     * @return 성공 응답 객체
     */
    public static InvitationVerifyResponse success(Long companyId, String companyName, String adminName) {
        return InvitationVerifyResponse.builder()
                .valid(true)
                .companyId(companyId)
                .companyName(companyName)
                .adminName(adminName)
                .build();
    }
    
    /**
     * 실패 응답 생성
     * @param errorMessage 오류 메시지
     * @return 실패 응답 객체
     */
    public static InvitationVerifyResponse fail(String errorMessage) {
        return InvitationVerifyResponse.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
} 