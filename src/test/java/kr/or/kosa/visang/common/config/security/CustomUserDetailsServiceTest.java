package kr.or.kosa.visang.common.config.security;

import kr.or.kosa.visang.common.config.security.exception.EmailNotVerifiedException;
import kr.or.kosa.visang.domain.admin.model.Admin;
import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.user.repository.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ClientMapper clientMapper;
    @Mock
    private AgentMapper agentMapper;
    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("고객 이메일 인증 완료 후 로그인 성공")
    void loadUserByUsername_clientSuccess() {
        String email = "client@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("CLIENT");

        Client client = Client.builder()
                .clientId(1L)
                .name("홍길동")
                .email(email)
                .password("encoded")
                .role("USER")
                .phoneNumber("010-1234-5678")
                .createdAt(LocalDateTime.now())
                .ssn("900101-1234567")
                .emailVerified(true)
                .build();

        when(clientMapper.findByEmail(email)).thenReturn(client);

        UserDetails details = userDetailsService.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals(email, details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시도 시 예외 발생")
    void loadUserByUsername_notFound() {
        String email = "notfound@example.com";
        when(userMapper.findUserTypeByEmail(email)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("이메일 미인증 상태의 고객 로그인 시도 시 예외 발생")
    void loadUserByUsername_emailNotVerified() {
        String email = "unverified@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("CLIENT");

        Client client = Client.builder()
                .clientId(2L)
                .name("김철수")
                .email(email)
                .password("encoded")
                .role("USER")
                .phoneNumber("010-8888-9999")
                .createdAt(LocalDateTime.now())
                .ssn("910101-2234567")
                .emailVerified(false) // 미인증 상태
                .build();

        when(clientMapper.findByEmail(email)).thenReturn(client);

        assertThrows(EmailNotVerifiedException.class, () -> userDetailsService.loadUserByUsername(email));
    }
    
    @Test
    @DisplayName("관리자 이메일 인증 완료 후 로그인 성공")
    void loadUserByUsername_adminSuccess() {
        String email = "admin@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("ADMIN");

        Admin admin = Admin.builder()
                .adminId(1L)
                .name("관리자")
                .email(email)
                .password("encoded")
                .role("ADMIN")
                .phoneNumber("010-2222-3333")
                .address("서울시 강남구")
                .companyId(100L)
                .createdAt(LocalDateTime.now())
                .emailVerified(true)
                .build();

        when(adminMapper.findByEmail(email)).thenReturn(admin);

        UserDetails details = userDetailsService.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals(email, details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        CustomUserDetails customDetails = (CustomUserDetails) details;
        assertEquals(admin.getAdminId(), customDetails.getUserId());
        assertEquals(admin.getCompanyId(), customDetails.getCompanyId());
        assertNull(customDetails.getClientId());
        assertNull(customDetails.getAgentId());
    }
    
    @Test
    @DisplayName("상담원 이메일 인증 완료 후 로그인 성공")
    void loadUserByUsername_agentSuccess() {
        String email = "agent@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("AGENT");

        Agent agent = Agent.builder()
                .agentId(1L)
                .name("상담원")
                .email(email)
                .password("encoded")
                .role("AGENT")
                .phoneNumber("010-4444-5555")
                .address("서울시 송파구")
                .companyId(100L)
                .state("ACTIVE") // 활성화 상태
                .createdAt(LocalDateTime.now())
                .emailVerified(true)
                .build();

        when(agentMapper.findByEmail(email)).thenReturn(agent);

        UserDetails details = userDetailsService.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals(email, details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT")));
        CustomUserDetails customDetails = (CustomUserDetails) details;
        assertEquals(agent.getAgentId(), customDetails.getUserId());
        assertEquals(agent.getCompanyId(), customDetails.getCompanyId());
        assertEquals(agent.getAgentId(), customDetails.getAgentId());
        assertNull(customDetails.getClientId());
    }
    
    @Test
    @DisplayName("비활성화된 상담원 계정 로그인 시도 시 예외 발생")
    void loadUserByUsername_inactiveAgent() {
        String email = "inactive@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("AGENT");

        Agent agent = Agent.builder()
                .agentId(2L)
                .name("비활성 상담원")
                .email(email)
                .password("encoded")
                .role("AGENT")
                .phoneNumber("010-6666-7777")
                .companyId(100L)
                .state("INACTIVE") // 비활성화 상태
                .createdAt(LocalDateTime.now())
                .emailVerified(true)
                .build();

        when(agentMapper.findByEmail(email)).thenReturn(agent);

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
    }
    
    @Test
    @DisplayName("승인 대기 중인 상담원 계정 로그인 시도 시 예외 발생")
    void loadUserByUsername_pendingAgent() {
        String email = "pending@example.com";

        when(userMapper.findUserTypeByEmail(email)).thenReturn("AGENT");

        Agent agent = Agent.builder()
                .agentId(3L)
                .name("대기중 상담원")
                .email(email)
                .password("encoded")
                .role("AGENT")
                .phoneNumber("010-7777-8888")
                .companyId(100L)
                .state("PENDING") // 승인 대기 상태
                .createdAt(LocalDateTime.now())
                .emailVerified(true)
                .build();

        when(agentMapper.findByEmail(email)).thenReturn(agent);

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
    }
} 