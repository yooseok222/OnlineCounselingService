package kr.or.kosa.visang.domain.company.service;

import kr.or.kosa.visang.domain.company.dto.InvitationRequest;
import kr.or.kosa.visang.domain.company.dto.InvitationResponse;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;

import java.util.List;

/**
 * 초대코드 관리 서비스 인터페이스
 */
public interface InvitationService {
    
    /**
     * 초대코드 생성
     * @param request 초대코드 생성 요청 정보
     * @return 생성된 초대코드 정보
     */
    InvitationResponse createInvitation(InvitationRequest request);
    
    /**
     * 초대코드 유효성 검증
     * @param invitationCode 검증할 초대코드
     * @return 초대코드 검증 결과 (유효한 경우 회사 ID, 회사명, 관리자명 포함)
     */
    InvitationVerifyResponse verifyInvitation(String invitationCode);
    
    /**
     * 관리자의 초대코드 목록 조회
     * @param adminId 관리자 ID
     * @return 초대코드 목록
     */
    List<InvitationResponse> getInvitationsByAdminId(Long adminId);
    
    /**
     * 회사의 초대코드 목록 조회
     * @param companyId 회사 ID
     * @return 초대코드 목록
     */
    List<InvitationResponse> getInvitationsByCompanyId(Long companyId);
    
    /**
     * 초대코드 삭제
     * @param invitationId 삭제할 초대코드 ID
     * @return 삭제 성공 여부
     */
    boolean deleteInvitation(Long invitationId);
} 