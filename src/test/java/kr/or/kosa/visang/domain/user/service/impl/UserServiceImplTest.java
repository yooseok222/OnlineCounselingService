package kr.or.kosa.visang.domain.user.service.impl;

import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.company.model.Company;
import kr.or.kosa.visang.domain.company.repository.CompanyMapper;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;
import kr.or.kosa.visang.domain.company.service.InvitationService;
import kr.or.kosa.visang.domain.user.dto.UserRegistrationRequest;
import kr.or.kosa.visang.domain.user.dto.UserResponse;
import kr.or.kosa.visang.domain.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private ClientMapper clientMapper;
    @Mock
    private AgentMapper agentMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private CompanyMapper companyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;
    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    }

    @Test
    @DisplayName("일반 사용자 회원가입 성공")
    void registerUserSuccess() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("홍길동")
                .email("user@example.com")
                .password("Password1!")
                .phoneNumber("010-1234-5678")
                .address("Seoul")
                .userType("USER")
                .ssn("900101-1234567")
                .build();

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(clientMapper.isPhoneNumberExists(anyString())).thenReturn(false);
        when(clientMapper.isSsnExists(request.getSsn())).thenReturn(false);

        // save 시에 clientId를 세팅해주는 더미 동작
        doAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            client.setClientId(1L);
            return 1;
        }).when(clientMapper).save(any(Client.class));

        // when
        UserResponse response = userService.register(request);

        // then
        assertNotNull(response);
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals(1L, response.getUserId());
    }

    @Test
    @DisplayName("이메일 중복으로 인한 회원가입 실패")
    void registerUserEmailDuplicated() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("홍길동")
                .email("duplicate@example.com")
                .password("Password1!")
                .phoneNumber("010-1111-2222")
                .address("Seoul")
                .userType("USER")
                .ssn("900101-1234567")
                .build();

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(true);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("이미 사용 중인 이메일"));
    }

    @Test
    @DisplayName("전화번호 중복으로 인한 회원가입 실패")
    void registerUserPhoneDuplicated() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("홍길동")
                .email("phoneDup@example.com")
                .password("Password1!")
                .phoneNumber("010-9999-8888")
                .address("Seoul")
                .userType("USER")
                .ssn("900101-1234567")
                .build();

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(clientMapper.isPhoneNumberExists(anyString())).thenReturn(true);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("이미 사용 중인 전화번호"));
    }

    @Test
    @DisplayName("주민등록번호 누락으로 인한 회원가입 실패")
    void registerUserMissingSsn() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("홍길동")
                .email("nosnn@example.com")
                .password("Password1!")
                .phoneNumber("010-0000-0000")
                .address("Seoul")
                .userType("USER")
                .build(); // ssn 누락

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("주민등록번호는 필수 입력 항목"));
    }

    @Test
    @DisplayName("주민등록번호 중복으로 인한 회원가입 실패")
    void registerUserSsnDuplicated() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("홍길동")
                .email("ssnDup@example.com")
                .password("Password1!")
                .phoneNumber("010-2222-3333")
                .address("Seoul")
                .userType("USER")
                .ssn("900101-1234567")
                .build();

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(clientMapper.isPhoneNumberExists(anyString())).thenReturn(false);
        when(clientMapper.isSsnExists(anyString())).thenReturn(true);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("이미 사용 중인 주민등록번호"));
    }

    @Test
    @DisplayName("관리자 회원가입 성공")
    void registerAdminSuccess() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("관리자")
                .email("admin@example.com")
                .password("Password1!")
                .phoneNumber("010-4444-5555")
                .address("Seoul")
                .userType("ADMIN")
                .companyName("테스트회사")
                .build();

        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isPhoneNumberExists(anyString())).thenReturn(false);

        doAnswer(invocation -> {
            Company companyParam = invocation.getArgument(0);
            // 가짜 PK 할당(reflection)
            java.lang.reflect.Field f = companyParam.getClass().getDeclaredField("companyId");
            f.setAccessible(true);
            f.set(companyParam, 100L);
            return 1;
        }).when(companyMapper).save(any(Company.class));

        // adminMapper.save 스텁
        doAnswer(invocation -> {
            return 1;
        }).when(adminMapper).save(any());

        // when
        UserResponse response = userService.register(request);

        // then
        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        assertEquals(request.getEmail(), response.getEmail());
    }

    @Test
    @DisplayName("상담원 회원가입 성공 (유효한 초대코드)")
    void registerAgentSuccess() {
        // given
        String invitationCode = "INVITE123";
        Long companyId = 100L;
        String companyName = "테스트회사";
        String adminName = "관리자명";

        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("상담원")
                .email("agent@example.com")
                .password("Password1!")
                .phoneNumber("010-5555-6666")
                .address("Seoul")
                .userType("AGENT")
                .invitationCode(invitationCode)
                .build();

        // 초대코드 검증 결과 모킹
        InvitationVerifyResponse verifyResponse = InvitationVerifyResponse.builder()
                .valid(true)
                .companyId(companyId)
                .companyName(companyName)
                .adminName(adminName)
                .build();

        when(invitationService.verifyInvitation(invitationCode)).thenReturn(verifyResponse);
        
        // 회사 정보 모킹
        Company company = Company.builder()
                .companyId(companyId)
                .companyName(companyName)
                .build();
        when(companyMapper.findById(companyId)).thenReturn(company);

        // 이메일 및 전화번호 중복 체크 모킹
        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isPhoneNumberExists(anyString())).thenReturn(false);

        // save 시에 agentId를 세팅해주는 더미 동작
        doAnswer(invocation -> {
            kr.or.kosa.visang.domain.agent.model.Agent agent = invocation.getArgument(0);
            // 가짜 PK 할당(reflection)
            java.lang.reflect.Field f = agent.getClass().getDeclaredField("agentId");
            f.setAccessible(true);
            f.set(agent, 200L);
            return 1;
        }).when(agentMapper).save(any(kr.or.kosa.visang.domain.agent.model.Agent.class));

        // when
        UserResponse response = userService.register(request);

        // then
        assertNotNull(response);
        assertEquals("AGENT", response.getRole());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(companyId, response.getCompanyId());
        assertEquals(companyName, response.getCompanyName());
        assertEquals("INACTIVE", response.getState()); // 초기 상태는 INACTIVE
    }

    @Test
    @DisplayName("상담원 회원가입 실패 (유효하지 않은 초대코드)")
    void registerAgentInvalidInvitationCode() {
        // given
        String invalidInvitationCode = "INVALID123";

        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("상담원")
                .email("agent@example.com")
                .password("Password1!")
                .phoneNumber("010-5555-6666")
                .address("Seoul")
                .userType("AGENT")
                .invitationCode(invalidInvitationCode)
                .build();

        // 초대코드 검증 결과 모킹 (유효하지 않음)
        InvitationVerifyResponse verifyResponse = InvitationVerifyResponse.builder()
                .valid(false)
                .errorMessage("유효하지 않은 초대코드입니다.")
                .build();

        when(invitationService.verifyInvitation(invalidInvitationCode)).thenReturn(verifyResponse);

        // 이메일 중복 체크 모킹
        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("유효하지 않은 초대코드입니다"));
    }

    @Test
    @DisplayName("상담원 회원가입 실패 (초대코드 누락)")
    void registerAgentMissingInvitationCode() {
        // given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("상담원")
                .email("agent@example.com")
                .password("Password1!")
                .phoneNumber("010-5555-6666")
                .address("Seoul")
                .userType("AGENT")
                // 초대코드 누락
                .build();

        // 이메일 중복 체크 모킹
        when(clientMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(adminMapper.isEmailExists(request.getEmail())).thenReturn(false);
        when(agentMapper.isEmailExists(request.getEmail())).thenReturn(false);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("초대코드는 필수 입력 항목입니다"));
    }
} 