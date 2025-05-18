package kr.or.kosa.visang.domain.agent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("상담원 모델 테스트")
public class AgentTest {

    @Test
    @DisplayName("유효한 정보로 상담원 생성")
    void createAgentWithValidInfo() {
        // given
        Long agentId = 1L;
        Long companyId = 100L;
        String state = "ACTIVE";
        
        // when
        Agent agent = Agent.builder()
                .agentId(agentId)
                .companyId(companyId)
                .name("상담원")
                .email("agent@example.com")
                .password("Password123!")
                .state(state)
                .phoneNumber("010-2345-6789")
                .address("서울시 서초구")
                .role("AGENT")
                .createdAt(LocalDateTime.now())
                .profileImageUrl("http://example.com/agent_profile.jpg")
                .build();
        
        // then
        assertNotNull(agent);
        assertEquals(agentId, agent.getAgentId());
        assertEquals(companyId, agent.getCompanyId());
        assertEquals(state, agent.getState());
        assertEquals("AGENT", agent.getRole());
    }
    
    @Test
    @DisplayName("상태 코드가 올바르지 않으면 예외 발생")
    void shouldThrowExceptionForInvalidState() {
        // given
        String invalidState = "UNKNOWN"; // 유효하지 않은 상태 코드
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Agent.builder()
                    .agentId(1L)
                    .companyId(100L)
                    .name("상담원")
                    .email("agent@example.com")
                    .password("Password123!")
                    .state(invalidState)
                    .phoneNumber("010-2345-6789")
                    .address("서울시 서초구")
                    .role("AGENT")
                    .createdAt(LocalDateTime.now())
                    .profileImageUrl("http://example.com/agent_profile.jpg")
                    .build()
                    .validateState();
        });
    }
    
    @Test
    @DisplayName("회사 ID가 없으면 예외 발생")
    void shouldThrowExceptionForNullCompanyId() {
        // given
        Long nullCompanyId = null;
        
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            Agent.builder()
                    .agentId(1L)
                    .companyId(nullCompanyId)
                    .name("상담원")
                    .email("agent@example.com")
                    .password("Password123!")
                    .state("ACTIVE")
                    .phoneNumber("010-2345-6789")
                    .address("서울시 서초구")
                    .role("AGENT")
                    .createdAt(LocalDateTime.now())
                    .profileImageUrl("http://example.com/agent_profile.jpg")
                    .build()
                    .validateCompanyId();
        });
    }
} 