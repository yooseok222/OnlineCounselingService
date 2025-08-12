package kr.or.kosa.visang.domain.client.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("고객 모델 테스트")
public class ClientTest {

    @Test
    @DisplayName("유효한 정보로 고객 생성")
    void createClientWithValidInfo() {
        // given
        Long userId = 1L;
        String ssn = "990101-1234567";
        
        // when
        Client client = Client.builder()
                .clientId(userId)
                .ssn(ssn)
                .name("고객")
                .email("client@example.com")
                .password("Password123!")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .profileImageUrl("http://example.com/profile.jpg")
                .build();
        
        // then
        assertNotNull(client);
        assertEquals(userId, client.getClientId());
        assertEquals(ssn, client.getSsn());
        assertEquals("USER", client.getRole());
    }
    
    @Test
    @DisplayName("주민등록번호 형식이 올바르지 않으면 예외 발생")
    void shouldThrowExceptionForInvalidSsn() {
        // given
        String invalidSsn = "1234567890"; // 유효하지 않은 주민등록번호 형식
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Client.builder()
                    .clientId(1L)
                    .ssn(invalidSsn)
                    .name("고객")
                    .email("client@example.com")
                    .password("Password123!")
                    .phoneNumber("010-1234-5678")
                    .address("서울시 강남구")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateSsn();
        });
    }
} 