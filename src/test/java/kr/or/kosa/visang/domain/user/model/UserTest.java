package kr.or.kosa.visang.domain.user.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("사용자 모델 테스트")
public class UserTest {

    @Test
    @DisplayName("유효한 이메일로 사용자 생성")
    void createUserWithValidEmail() {
        // given
        String validEmail = "test@example.com";
        
        // when
        User user = User.builder()
                .id(1L)
                .name("테스트사용자")
                .email(validEmail)
                .password("Password123!")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();
        
        // then
        assertNotNull(user);
        assertEquals(validEmail, user.getEmail());
        assertEquals("USER", user.getRole());
    }
    
    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 예외 발생")
    void shouldThrowExceptionForInvalidEmail() {
        // given
        String invalidEmail = "invalid-email";
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            User.builder()
                    .id(1L)
                    .name("테스트사용자")
                    .email(invalidEmail)
                    .password("Password123!")
                    .phoneNumber("010-1234-5678")
                    .address("서울시 강남구")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validateEmail();
        });
    }
    
    @Test
    @DisplayName("비밀번호 형식이 올바르지 않으면 예외 발생")
    void shouldThrowExceptionForInvalidPassword() {
        // given
        String weakPassword = "123456";
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            User.builder()
                    .id(1L)
                    .name("테스트사용자")
                    .email("test@example.com")
                    .password(weakPassword)
                    .phoneNumber("010-1234-5678")
                    .address("서울시 강남구")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validatePassword();
        });
    }
    
    @Test
    @DisplayName("전화번호 형식이 올바르지 않으면 예외 발생")
    void shouldThrowExceptionForInvalidPhoneNumber() {
        // given
        String invalidPhoneNumber = "123-456";
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            User.builder()
                    .id(1L)
                    .name("테스트사용자")
                    .email("test@example.com")
                    .password("Password123!")
                    .phoneNumber(invalidPhoneNumber)
                    .address("서울시 강남구")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .build()
                    .validatePhoneNumber();
        });
    }
} 