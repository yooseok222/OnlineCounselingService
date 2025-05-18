package kr.or.kosa.visang.domain.company.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("회사 모델 테스트")
public class CompanyTest {

    @Test
    @DisplayName("유효한 정보로 회사 생성")
    void createCompanyWithValidInfo() {
        // given
        Long companyId = 100L;
        String companyName = "비상교육";
        LocalDateTime now = LocalDateTime.now();
        
        // when
        Company company = Company.builder()
                .companyId(companyId)
                .companyName(companyName)
                .createdAt(now)
                .build();
        
        // then
        assertNotNull(company);
        assertEquals(companyId, company.getCompanyId());
        assertEquals(companyName, company.getCompanyName());
        assertEquals(now, company.getCreatedAt());
    }
    
    @Test
    @DisplayName("회사명이 없으면 예외 발생")
    void shouldThrowExceptionForNullCompanyName() {
        // given
        String nullCompanyName = null;
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Company.builder()
                    .companyId(100L)
                    .companyName(nullCompanyName)
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateCompanyName();
        });
    }
    
    @Test
    @DisplayName("회사명이 너무 짧으면 예외 발생")
    void shouldThrowExceptionForShortCompanyName() {
        // given
        String shortCompanyName = "비";  // 1자 (최소 2자 이상 필요)
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Company.builder()
                    .companyId(100L)
                    .companyName(shortCompanyName)
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateCompanyName();
        });
    }
} 