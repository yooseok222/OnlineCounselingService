package kr.or.kosa.visang.domain.invitation.service;

import kr.or.kosa.visang.domain.invitation.model.Invitation;
import kr.or.kosa.visang.domain.invitation.repository.InvitationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {
    
    private final InvitationMapper invitationMapper;
    
    /**
     * 클라이언트용 초대코드 검증
     * @param code 초대코드
     * @param contractId 계약 ID
     * @return 검증된 초대 정보
     * @throws IllegalArgumentException 검증 실패 시
     */
    @Transactional(readOnly = true)
    public Invitation validateInvitationForClient(String code, String contractId) {
        log.info("클라이언트 초대코드 검증 시작 - code: {}, contractId: {}", code, contractId);
        
        // 1. 초대코드로 invitation 조회
        Invitation invitation = invitationMapper.findByCode(code);
        
        if (invitation == null) {
            log.warn("유효하지 않은 초대코드: {}", code);
            throw new IllegalArgumentException("유효하지 않은 초대코드입니다.");
        }
        
        // 2. contract_id 매칭 확인
        if (!invitation.getContractId().toString().equals(contractId)) {
            log.warn("계약 정보 불일치 - invitation contractId: {}, 요청 contractId: {}", 
                    invitation.getContractId(), contractId);
            throw new IllegalArgumentException("계약 정보가 일치하지 않습니다.");
        }
        
        // 3. 만료시간 확인
        if (invitation.getExpiredTime() != null && invitation.getExpiredTime().isBefore(LocalDateTime.now())) {
            log.warn("만료된 초대링크 - code: {}, expiredTime: {}", code, invitation.getExpiredTime());
            throw new IllegalArgumentException("초대링크가 만료되었습니다.");
        }
        
        log.info("클라이언트 초대코드 검증 성공 - invitationId: {}, contractId: {}", 
                invitation.getInvitationId(), invitation.getContractId());
        
        return invitation;
    }
    
    /**
     * 초대 상태를 '사용중'으로 업데이트
     * @param invitationId 초대 ID
     */
    @Transactional
    public void markAsInUse(Long invitationId) {
        log.info("초대 상태를 '사용중'으로 업데이트 - invitationId: {}", invitationId);
        // 현재 invitation 테이블에 status 컬럼이 없으므로 로그만 남김
        // 향후 status 컬럼 추가 시 실제 업데이트 쿼리 실행
        // invitationMapper.updateStatus(invitationId, "IN_USE");
    }
    
    /**
     * 초대 상태를 '사용완료'로 업데이트
     * @param invitationId 초대 ID
     */
    @Transactional
    public void markAsUsed(Long invitationId) {
        log.info("초대 상태를 '사용완료'로 업데이트 - invitationId: {}", invitationId);
        // 현재 invitation 테이블에 status 컬럼이 없으므로 로그만 남김
        // 향후 status 컬럼 추가 시 실제 업데이트 쿼리 실행
        // invitationMapper.updateStatus(invitationId, "USED");
    }
    
    /**
     * 초대코드로 초대 정보 조회
     * @param code 초대코드
     * @return 초대 정보 (없으면 null)
     */
    @Transactional(readOnly = true)
    public Invitation findByCode(String code) {
        log.debug("초대코드로 초대 정보 조회 - code: {}", code);
        return invitationMapper.findByCode(code);
    }
    
    /**
     * 초대 정보 생성
     * @param invitation 초대 정보
     */
    @Transactional
    public void createInvitation(Invitation invitation) {
        log.info("새 초대 정보 생성 - contractId: {}, code: {}", 
                invitation.getContractId(), invitation.getInvitationCode());
        invitationMapper.insertInvitation(invitation);
    }
    
    /**
     * 계약 ID로 초대 정보 삭제
     * @param contractId 계약 ID
     */
    @Transactional
    public void deleteByContractId(Long contractId) {
        log.info("계약별 초대 정보 삭제 - contractId: {}", contractId);
        invitationMapper.deleteByContractId(contractId);
    }
} 