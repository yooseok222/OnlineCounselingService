package kr.or.kosa.visang.domain.company.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("초대코드 모델 테스트")
public class InvitationTest {

    @Test
    @DisplayName("유효한 정보로 초대코드 생성")
    void createInvitationWithValidInfo() {
        // given
        Long invitationId = 1L;
        String invitationCode = "INVITE123456";
        Long companyId = 100L;
        String adminName = "관리자";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredTime = now.plusDays(7);
        
        // when
        Invitation invitation = Invitation.builder()
                .invitationId(invitationId)
                .invitationCode(invitationCode)
                .companyId(companyId)
                .adminName(adminName)
                .expiredTime(expiredTime)
                .createdAt(now)
                .build();
        
        // then
        assertNotNull(invitation);
        assertEquals(invitationId, invitation.getInvitationId());
        assertEquals(invitationCode, invitation.getInvitationCode());
        assertEquals(companyId, invitation.getCompanyId());
        assertEquals(adminName, invitation.getAdminName());
        assertEquals(expiredTime, invitation.getExpiredTime());
        assertEquals(now, invitation.getCreatedAt());
        assertFalse(invitation.isExpired(now));
        assertTrue(invitation.isExpired(now.plusDays(8)));
    }
    
    @Test
    @DisplayName("초대코드가 없으면 예외 발생")
    void shouldThrowExceptionForNullInvitationCode() {
        // given
        String nullInvitationCode = null;
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Invitation.builder()
                    .invitationId(1L)
                    .invitationCode(nullInvitationCode)
                    .companyId(100L)
                    .adminName("관리자")
                    .expiredTime(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateInvitationCode();
        });
    }
    
    @Test
    @DisplayName("만료시간이 현재보다 이전이면 만료됨으로 판단")
    void shouldBeExpiredWhenExpiredTimeBeforeNow() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusDays(1);
        
        // when
        Invitation invitation = Invitation.builder()
                .invitationId(1L)
                .invitationCode("INVITE123456")
                .companyId(100L)
                .adminName("관리자")
                .expiredTime(pastTime)
                .createdAt(now.minusDays(7))
                .build();
        
        // then
        assertTrue(invitation.isExpired(now));
    }
} 