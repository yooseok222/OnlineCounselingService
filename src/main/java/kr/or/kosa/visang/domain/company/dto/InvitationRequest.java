package kr.or.kosa.visang.domain.company.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 초대코드 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {
    
    /**
     * 관리자 ID
     */
    @NotNull(message = "관리자 ID는 필수 입력 항목입니다.")
    private Long adminId;
    
    /**
     * 관리자 이름
     */
    @NotNull(message = "관리자 이름은 필수 입력 항목입니다.")
    private String adminName;
    
    /**
     * 회사 ID
     */
    @NotNull(message = "회사 ID는 필수 입력 항목입니다.")
    private Long companyId;
    
    /**
     * 만료 기간 (일 단위)
     */
    @Min(value = 1, message = "만료 기간은 최소 1일 이상이어야 합니다.")
    private int expirationDays;
} 