package kr.or.kosa.visang.domain.company.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
public class Company {
    private Long companyId;
    private String companyName;
    private LocalDateTime createdAt;

    // 회사명 최소 길이
    private static final int MIN_COMPANY_NAME_LENGTH = 2;

    @Builder
    public Company(Long companyId, String companyName, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.createdAt = createdAt;
    }

    /**
     * 회사명 유효성 검증
     * @return 검증된 Company 객체
     * @throws IllegalArgumentException 회사명이 null이거나 최소 길이보다 짧은 경우
     */
    public Company validateCompanyName() {
        if (companyName == null || companyName.length() < MIN_COMPANY_NAME_LENGTH) {
            throw new IllegalArgumentException("회사명은 " + MIN_COMPANY_NAME_LENGTH + "자 이상이어야 합니다.");
        }
        return this;
    }

    /**
     * 회사 ID 유효성 검증
     * @return 검증된 Company 객체
     * @throws IllegalArgumentException 회사 ID가 null인 경우
     */
    public Company validateCompanyId() {
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID는 필수 항목입니다.");
        }
        return this;
    }

    /**
     * 모든 필드의 유효성 검증 (신규 생성용)
     * companyId 는 DB 자동 생성이므로 companyName 만 검증한다.
     */
    public Company validate() {
        return validateCompanyName();
    }

    /**
     * 기존 엔티티에 대한 전체 필드 유효성 검증 (companyId 포함)
     */
    public Company validateForExisting() {
        return validateCompanyName().validateCompanyId();
    }
} 