package kr.or.kosa.visang.domain.company.service;

import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.company.dto.InvitationRequest;
import kr.or.kosa.visang.domain.company.dto.InvitationResponse;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;
import kr.or.kosa.visang.domain.company.model.Company;
import kr.or.kosa.visang.domain.company.repository.CompanyMapper;
import kr.or.kosa.visang.domain.company.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("초대코드 서비스 테스트")
class InvitationServiceTest {

    @Mock
    private CompanyMapper companyMapper;
    
    @Mock
    private AdminMapper adminMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("초대코드 생성 성공")
    void createInvitationSuccess() {
        // given
        Long companyId = 100L;
        Long adminId = 1L;
        String adminName = "관리자";
        int expirationDays = 7;

        InvitationRequest request = InvitationRequest.builder()
                .companyId(companyId)
                .adminId(adminId)
                .adminName(adminName)
                .expirationDays(expirationDays)
                .build();

        Company company = Company.builder()
                .companyId(companyId)
                .companyName("테스트회사")
                .build();

        when(companyMapper.findById(companyId)).thenReturn(company);
        
        // Redis 관련 모킹은 최소화하여 테스트가 불필요하게 실패하지 않도록 함

        // when
        InvitationResponse response = invitationService.createInvitation(request);

        // then
        assertNotNull(response);
        assertNotNull(response.getInvitationCode());
        assertEquals(companyId, response.getCompanyId());
        assertEquals(company.getCompanyName(), response.getCompanyName());
        assertEquals(adminId, response.getAdminId());
        assertEquals(adminName, response.getAdminName());
        assertFalse(response.isExpired());
        
        // 단순히 메서드 호출 여부만 확인
        verify(valueOperations).set(anyString(), any(), anyLong(), any());
        verify(setOperations, times(2)).add(anyString(), any());
    }

    @Test
    @DisplayName("유효한 초대코드 검증 성공")
    void verifyInvitationValid() {
        // given
        String invitationCode = "INVITE123";
        Long companyId = 100L;
        String companyName = "테스트회사";
        String adminName = "관리자";
        
        InvitationResponse storedInvitation = InvitationResponse.builder()
                .invitationId(1L)
                .invitationCode(invitationCode)
                .companyId(companyId)
                .companyName(companyName)
                .adminName(adminName)
                .expiredTime(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .expired(false)
                .build();
        
        when(valueOperations.get(anyString())).thenReturn(storedInvitation);
        
        // 회사 정보 조회 모킹 추가
        Company company = Company.builder()
                .companyId(companyId)
                .companyName(companyName)
                .build();
        when(companyMapper.findById(companyId)).thenReturn(company);
        
        // when
        InvitationVerifyResponse response = invitationService.verifyInvitation(invitationCode);
        
        // then
        assertTrue(response.isValid());
        assertEquals(companyId, response.getCompanyId());
        assertEquals(companyName, response.getCompanyName());
        assertEquals(adminName, response.getAdminName());
        assertNull(response.getErrorMessage());
    }
    
    @Test
    @DisplayName("유효하지 않은 초대코드 검증 실패")
    void verifyInvitationInvalid() {
        // given
        String invitationCode = "INVALID123";
        
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // when
        InvitationVerifyResponse response = invitationService.verifyInvitation(invitationCode);
        
        // then
        assertFalse(response.isValid());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("유효하지 않은 초대코드"));
    }
    
    @Test
    @DisplayName("만료된 초대코드 검증 실패")
    void verifyInvitationExpired() {
        // given
        String invitationCode = "EXPIRED123";
        
        InvitationResponse storedInvitation = InvitationResponse.builder()
                .invitationId(2L)
                .invitationCode(invitationCode)
                .companyId(200L)
                .companyName("만료테스트회사")
                .adminName("만료관리자")
                .expiredTime(LocalDateTime.now().minusDays(1)) // 이미 만료됨
                .createdAt(LocalDateTime.now().minusDays(8))
                .expired(true)
                .build();
        
        when(valueOperations.get(anyString())).thenReturn(storedInvitation);
        
        // when
        InvitationVerifyResponse response = invitationService.verifyInvitation(invitationCode);
        
        // then
        assertFalse(response.isValid());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("만료된 초대코드"));
    }
    
    @Test
    @DisplayName("관리자 ID로 초대코드 목록 조회 성공")
    void getInvitationsByAdminIdSuccess() {
        // given
        Long adminId = 1L;
        Set<Object> codeSet = new HashSet<>(Arrays.asList("CODE1", "CODE2", "CODE3"));
        
        InvitationResponse inv1 = InvitationResponse.builder()
                .invitationId(1L)
                .invitationCode("CODE1")
                .companyId(100L)
                .build();
        
        InvitationResponse inv2 = InvitationResponse.builder()
                .invitationId(2L)
                .invitationCode("CODE2")
                .companyId(100L)
                .build();
        
        InvitationResponse inv3 = InvitationResponse.builder()
                .invitationId(3L)
                .invitationCode("CODE3")
                .companyId(100L)
                .build();
        
        when(setOperations.members(anyString())).thenReturn(codeSet);
        when(valueOperations.get(contains("CODE1"))).thenReturn(inv1);
        when(valueOperations.get(contains("CODE2"))).thenReturn(inv2);
        when(valueOperations.get(contains("CODE3"))).thenReturn(inv3);
        
        // when
        List<InvitationResponse> invitations = invitationService.getInvitationsByAdminId(adminId);
        
        // then
        assertNotNull(invitations);
        assertEquals(3, invitations.size());
    }
    
    @Test
    @DisplayName("회사 ID로 초대코드 목록 조회 성공")
    void getInvitationsByCompanyIdSuccess() {
        // given
        Long companyId = 100L;
        Set<Object> codeSet = new HashSet<>(Arrays.asList("CODE1", "CODE2"));
        
        InvitationResponse inv1 = InvitationResponse.builder()
                .invitationId(1L)
                .invitationCode("CODE1")
                .companyId(companyId)
                .build();
        
        InvitationResponse inv2 = InvitationResponse.builder()
                .invitationId(2L)
                .invitationCode("CODE2")
                .companyId(companyId)
                .build();
        
        when(setOperations.members(anyString())).thenReturn(codeSet);
        when(valueOperations.get(contains("CODE1"))).thenReturn(inv1);
        when(valueOperations.get(contains("CODE2"))).thenReturn(inv2);
        
        // when
        List<InvitationResponse> invitations = invitationService.getInvitationsByCompanyId(companyId);
        
        // then
        assertNotNull(invitations);
        assertEquals(2, invitations.size());
    }
    
    @Test
    @DisplayName("초대코드 삭제 성공")
    void deleteInvitationSuccess() {
        // given
        Long invitationId = 1L;
        String invitationCode = "DELETE123";
        Long companyId = 100L;
        Long adminId = 1L;
        
        InvitationResponse storedInvitation = InvitationResponse.builder()
                .invitationId(invitationId)
                .invitationCode(invitationCode)
                .companyId(companyId)
                .adminId(adminId)
                .build();
        
        String detailKey = "invitation:detail:" + invitationCode;
        
        // redisTemplate.keys() 모킹
        Set<String> keys = new HashSet<>();
        keys.add(detailKey);
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        
        // 간단하게 모킹
        when(valueOperations.get(anyString())).thenReturn(storedInvitation);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        
        // when
        boolean result = invitationService.deleteInvitation(invitationId);
        
        // then
        assertTrue(result);
        verify(redisTemplate).delete(anyString());
        verify(setOperations, times(2)).remove(anyString(), anyString());
    }
    
    @Test
    @DisplayName("존재하지 않는 초대코드 삭제 실패")
    void deleteInvitationNotFound() {
        // given
        Long invitationId = 999L;
        
        // redisTemplate.keys() 모킹 추가 - 빈 세트 반환
        Set<String> keys = new HashSet<>();
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        
        // when
        boolean result = invitationService.deleteInvitation(invitationId);
        
        // then
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
        verify(setOperations, never()).remove(anyString(), any());
    }
} 