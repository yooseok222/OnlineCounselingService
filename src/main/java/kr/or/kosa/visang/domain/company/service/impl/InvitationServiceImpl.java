package kr.or.kosa.visang.domain.company.service.impl;

import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.company.dto.InvitationRequest;
import kr.or.kosa.visang.domain.company.dto.InvitationResponse;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;
import kr.or.kosa.visang.domain.company.model.Company;
import kr.or.kosa.visang.domain.company.repository.CompanyMapper;
import kr.or.kosa.visang.domain.company.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final CompanyMapper companyMapper;
    private final AdminMapper adminMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key Prefixes (더 명확한 네이밍)
    private static final String INV_PREFIX_DETAIL = "invitation:detail:";         // 초대코드 → 상세 객체
    private static final String INV_PREFIX_BY_ADMIN = "invitation:list:byAdmin:"; // 관리자별 코드 Set
    private static final String INV_PREFIX_BY_COMPANY = "invitation:list:byCompany:"; // 회사별 코드 Set

    // 기본 만료 (일): 명시하지 않으면 7일
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    
    @Override
    public InvitationResponse createInvitation(InvitationRequest request) {
        // 초대코드 생성 (UUID의 앞 8자리 사용)
        String invitationCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // 만료 시간 계산 (요청값 없으면 기본 7일)
        LocalDateTime now = LocalDateTime.now();
        int expDays = (request.getExpirationDays() <= 0)
                ? DEFAULT_EXPIRATION_DAYS : request.getExpirationDays();
        LocalDateTime expiredTime = now.plusDays(expDays);
        
        // 회사 정보 조회
        Company company = companyMapper.findById(request.getCompanyId());
        if (company == null) {
            throw new IllegalArgumentException("유효하지 않은 회사 정보입니다.");
        }
        
        String companyName = company.getCompanyName();
        
        // 초대코드 정보 구성
        InvitationResponse invitationResponse = InvitationResponse.builder()
                .invitationId(System.currentTimeMillis()) // ID 대신 타임스탬프 사용
                .invitationCode(invitationCode)
                .companyId(request.getCompanyId())
                .companyName(companyName)
                .adminId(request.getAdminId())
                .adminName(request.getAdminName())
                .expiredTime(expiredTime)
                .createdAt(now)
                .expired(false)
                .build();
        
        // 초대코드 Redis 저장 (만료 시간과 함께)
        String codeKey = INV_PREFIX_DETAIL + invitationCode;
        long expirationMillis = java.time.Duration.between(now, expiredTime).toMillis();
        
        // Redis에 초대코드 정보 저장
        redisTemplate.opsForValue().set(codeKey, invitationResponse, expirationMillis, TimeUnit.MILLISECONDS);
        
        // 관리자 ID 별로 초대코드 관리
        String adminKey = INV_PREFIX_BY_ADMIN + request.getAdminId();
        redisTemplate.opsForSet().add(adminKey, invitationCode);
        
        // 회사 ID 별로 초대코드 관리
        String companyKey = INV_PREFIX_BY_COMPANY + request.getCompanyId();
        redisTemplate.opsForSet().add(companyKey, invitationCode);
        
        return invitationResponse;
    }

    @Override
    public InvitationVerifyResponse verifyInvitation(String invitationCode) {
        // Redis에서 초대코드 정보 조회
        String codeKey = INV_PREFIX_DETAIL + invitationCode;
        InvitationResponse invitation = (InvitationResponse) redisTemplate.opsForValue().get(codeKey);
        
        if (invitation == null) {
            return InvitationVerifyResponse.fail("유효하지 않은 초대코드입니다.");
        }
        
        // 만료 여부 확인 (Redis TTL로 자동 관리되지만 추가 확인)
        if (invitation.getExpiredTime().isBefore(LocalDateTime.now())) {
            return InvitationVerifyResponse.fail("만료된 초대코드입니다.");
        }
        
        // 회사 정보 조회
        Company company = companyMapper.findById(invitation.getCompanyId());
        if (company == null) {
            return InvitationVerifyResponse.fail("유효하지 않은 회사 정보입니다.");
        }
        
        return InvitationVerifyResponse.success(
                invitation.getCompanyId(),
                company.getCompanyName(),
                invitation.getAdminName()
        );
    }

    @Override
    public List<InvitationResponse> getInvitationsByAdminId(Long adminId) {
        // 관리자 ID로 초대코드 목록 조회
        String adminKey = INV_PREFIX_BY_ADMIN + adminId;
        Set<Object> invitationCodes = redisTemplate.opsForSet().members(adminKey);
        
        if (invitationCodes == null || invitationCodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 각 초대코드에 대한 상세 정보 조회
        return invitationCodes.stream()
                .map(code -> (String) code)
                .map(code -> (InvitationResponse) redisTemplate.opsForValue().get(INV_PREFIX_DETAIL + code))
                .filter(invitation -> invitation != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvitationResponse> getInvitationsByCompanyId(Long companyId) {
        // 회사 ID로 초대코드 목록 조회
        String companyKey = INV_PREFIX_BY_COMPANY + companyId;
        Set<Object> invitationCodes = redisTemplate.opsForSet().members(companyKey);
        
        if (invitationCodes == null || invitationCodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 각 초대코드에 대한 상세 정보 조회
        return invitationCodes.stream()
                .map(code -> (String) code)
                .map(code -> (InvitationResponse) redisTemplate.opsForValue().get(INV_PREFIX_DETAIL + code))
                .filter(invitation -> invitation != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteInvitation(Long invitationId) {
        // invitationId로는 Redis에서 직접 찾을 수 없으므로
        // 관리자와 회사별 모든 초대코드를 확인해야 함
        // 실제 구현에서는 비효율적일 수 있음
        
        // 모든 관리자의 초대코드 목록 조회
        Set<String> keys = redisTemplate.keys(INV_PREFIX_DETAIL + "*");
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        
        for (String key : keys) {
            InvitationResponse invitation = (InvitationResponse) redisTemplate.opsForValue().get(key);
            if (invitation != null && invitation.getInvitationId().equals(invitationId)) {
                // 초대코드 발견
                String invitationCode = invitation.getInvitationCode();
                
                // 초대코드 삭제
                redisTemplate.delete(INV_PREFIX_DETAIL + invitationCode);
                
                // 관리자 목록에서 삭제
                String adminKey = INV_PREFIX_BY_ADMIN + invitation.getAdminId();
                redisTemplate.opsForSet().remove(adminKey, invitationCode);
                
                // 회사 목록에서 삭제
                String companyKey = INV_PREFIX_BY_COMPANY + invitation.getCompanyId();
                redisTemplate.opsForSet().remove(companyKey, invitationCode);
                
                return true;
            }
        }
        
        return false;
    }
} 