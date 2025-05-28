package kr.or.kosa.visang.domain.invitation.controller;

import kr.or.kosa.visang.domain.invitation.service.InvitationService;
import kr.or.kosa.visang.domain.invitation.model.Invitation;
import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class InvitationController {
    
    private final InvitationService invitationService;
    
    /**
     * 고객 초대링크 처리 (로그인 필요)
     * URL: /client/invitation?code=xxx&contractId=yyy&session=zzz
     */
    @GetMapping("/invitation")
    public String processInvitation(
            @RequestParam String code, 
            @RequestParam String contractId,
            @RequestParam String session,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession httpSession,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            log.info("고객 초대링크 처리 시작 - code: {}, contractId: {}, session: {}, 로그인 사용자: {}", 
                    code, contractId, session, userDetails.getUsername());
            
            // 1. 로그인 사용자가 CLIENT 역할인지 확인
            if (!"USER".equals(userDetails.getRole())) {
                log.warn("CLIENT 역할이 아닌 사용자의 초대링크 접근 시도 - 사용자: {}, 역할: {}", 
                        userDetails.getUsername(), userDetails.getRole());
                redirectAttributes.addFlashAttribute("errorMessage", "고객만 초대링크를 사용할 수 있습니다.");
                return "redirect:/invitation/error";
            }
            
            // 2. 초대코드 검증
            Invitation invitation = invitationService.validateInvitationForClient(code, contractId);
            
            // 3. 서버 IP 획득
            String serverIp = request.getServerName();
            String serverPort = String.valueOf(request.getServerPort());
            String serverUrl = request.getScheme() + "://" + serverIp + ":" + serverPort;
            
            log.info("서버 정보 - IP: {}, Port: {}, URL: {}", serverIp, serverPort, serverUrl);
            
            // 4. 상담 세션 ID 생성 (contractId 기반으로 결정론적 생성 - 상담원과 동일)
            String consultingSessionId = generateSessionId(Long.valueOf(contractId));
            
            log.info("고객이 사용할 세션 ID 생성: {}", consultingSessionId);
            
            // 5. 세션에 검증된 정보 저장
            httpSession.setAttribute("validatedInvitation", invitation);
            httpSession.setAttribute("contractId", invitation.getContractId());
            httpSession.setAttribute("consultingSessionId", consultingSessionId);
            httpSession.setAttribute("serverUrl", serverUrl);
            httpSession.setAttribute("clientAccess", true);
            httpSession.setAttribute("loggedInClientId", userDetails.getClientId());
            
            log.info("세션에 저장 완료 - contractId: {}, sessionId: {}, clientId: {}", 
                    invitation.getContractId(), consultingSessionId, userDetails.getClientId());
            
            // 6. 초대 상태를 '사용중'으로 업데이트
            invitationService.markAsInUse(invitation.getInvitationId());
            
            log.info("고객 초대링크 처리 완료 - 클라이언트 입장 페이지로 이동");

            return "redirect:/client-entry";
            
        } catch (Exception e) {
            log.error("초대링크 처리 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invitation/error";
        }
    }
    
    /**
     * contractId 기반으로 항상 동일한 세션 ID 생성 (AgentService와 동일한 로직)
     */
    private String generateSessionId(Long contractId) {
        return "session_" + contractId + "_fixed";
    }
    
    /**
     * 초대링크 에러 페이지
     */
    @GetMapping("/invitation-error")
    public String invitationError(Model model) {
        return "redirect:/invitation/error";
    }
    
    /**
     * 초대링크 에러 페이지 (새 경로)
     */
    @GetMapping("/error")
    public String showClientInvitationError(Model model) {
        return "invitation/error";
    }
    
    /**
     * 랜덤 문자열 생성 헬퍼 메서드 (안전한 버전)
     */
    private String generateRandomString() {
        // UUID를 사용한 안전한 랜덤 문자열 생성
        return java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }
} 