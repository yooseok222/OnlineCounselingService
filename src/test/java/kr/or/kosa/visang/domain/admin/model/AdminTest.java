package kr.or.kosa.visang.domain.admin.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("관리자 모델 테스트")
public class AdminTest {

    @Test
    @DisplayName("유효한 정보로 관리자 생성")
    void createAdminWithValidInfo() {
        // given
        Long adminId = 1L;
        Long companyId = 100L;
        
        // when
        Admin admin = Admin.builder()
                .adminId(adminId)
                .companyId(companyId)
                .name("관리자")
                .email("admin@example.com")
                .password("Password123!")
                .phoneNumber("010-3456-7890")
                .address("서울시 송파구")
                .role("ADMIN")
                .createdAt(LocalDateTime.now())
                .build();
        
        // then
        assertNotNull(admin);
        assertEquals(adminId, admin.getAdminId());
        assertEquals(companyId, admin.getCompanyId());
        assertEquals("ADMIN", admin.getRole());
    }
    
    @Test
    @DisplayName("회사 ID가 없으면 예외 발생")
    void shouldThrowExceptionForNullCompanyId() {
        // given
        Long nullCompanyId = null;
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Admin.builder()
                    .adminId(1L)
                    .companyId(nullCompanyId)
                    .name("관리자")
                    .email("admin@example.com")
                    .password("Password123!")
                    .phoneNumber("010-3456-7890")
                    .address("서울시 송파구")
                    .role("ADMIN")
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateCompanyId();
        });
    }
} 