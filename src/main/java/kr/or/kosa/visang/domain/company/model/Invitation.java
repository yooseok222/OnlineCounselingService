package kr.or.kosa.visang.domain.company.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
public class Invitation {
    private Long invitationId;
    private String invitationCode;
    private Long companyId;
    private String adminName;
    private LocalDateTime expiredTime;
    private LocalDateTime createdAt;

    @Builder
    public Invitation(Long invitationId, String invitationCode, Long companyId, 
                      String adminName, LocalDateTime expiredTime, LocalDateTime createdAt) {
        this.invitationId = invitationId;
        this.invitationCode = invitationCode;
        this.companyId = companyId;
        this.adminName = adminName;
        this.expiredTime = expiredTime;
        this.createdAt = createdAt;
    }

    /**
     * 초대코드 유효성 검증
     * @return 검증된 Invitation 객체
     * @throws IllegalArgumentException 초대코드가 null이거나 빈 문자열인 경우
     */
    public Invitation validateInvitationCode() {
        if (invitationCode == null || invitationCode.isEmpty()) {
            throw new IllegalArgumentException("초대코드는 필수 항목입니다.");
        }
        return this;
    }

    /**
     * 회사 ID 유효성 검증
     * @return 검증된 Invitation 객체
     * @throws IllegalArgumentException 회사 ID가 null인 경우
     */
    public Invitation validateCompanyId() {
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID는 필수 항목입니다.");
        }
        return this;
    }

    /**
     * 관리자명 유효성 검증
     * @return 검증된 Invitation 객체
     * @throws IllegalArgumentException 관리자명이 null이거나 빈 문자열인 경우
     */
    public Invitation validateAdminName() {
        if (adminName == null || adminName.isEmpty()) {
            throw new IllegalArgumentException("관리자명은 필수 항목입니다.");
        }
        return this;
    }

    /**
     * 초대코드 만료 여부 확인
     * @param currentTime 현재 시간
     * @return 만료 여부 (true: 만료됨, false: 유효함)
     */
    public boolean isExpired(LocalDateTime currentTime) {
        return expiredTime != null && currentTime.isAfter(expiredTime);
    }

    /**
     * 모든 필드의 유효성 검증
     * @return 검증된 Invitation 객체
     */
    public Invitation validate() {
        return validateInvitationCode().validateCompanyId().validateAdminName();
    }
} 