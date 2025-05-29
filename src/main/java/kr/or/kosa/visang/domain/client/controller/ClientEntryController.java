package kr.or.kosa.visang.domain.client.controller;

import kr.or.kosa.visang.domain.invitation.service.InvitationService;
import kr.or.kosa.visang.domain.invitation.model.Invitation;
import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientEntryController {

    private final InvitationService invitationService;

    /**
     * 고객 입장 페이지 제공 (로그인 필요)
     * @return 고객 입장 뷰 이름
     */
    @GetMapping("/client-entry")
    public String clientEntry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session, 
            Model model) {
        
        log.info("=== ClientEntry 접근 ===");
        log.info("로그인 사용자: {}", userDetails.getUsername());
        log.info("현재 세션 ID: {}", session.getId());
        
        // 세션 데이터 확인
        Invitation validatedInvitation = (Invitation) session.getAttribute("validatedInvitation");
        Boolean clientAccess = (Boolean) session.getAttribute("clientAccess");
        Long contractId = (Long) session.getAttribute("contractId");
        String consultingSessionId = (String) session.getAttribute("consultingSessionId");
        String serverUrl = (String) session.getAttribute("serverUrl");
        Long loggedInClientId = (Long) session.getAttribute("loggedInClientId");
        
        log.info("세션 데이터 확인:");
        log.info("- validatedInvitation: {}", validatedInvitation != null ? "존재" : "null");
        log.info("- clientAccess: {}", clientAccess);
        log.info("- contractId: {}", contractId);
        log.info("- consultingSessionId: {}", consultingSessionId);
        log.info("- serverUrl: {}", serverUrl);
        log.info("- loggedInClientId: {}", loggedInClientId);
        
        // 초대링크로 온 경우 세션 검증
        if (validatedInvitation != null && Boolean.TRUE.equals(clientAccess)) {
            // 초대링크로 온 경우 - 검증된 정보를 모델에 추가
            model.addAttribute("contractId", contractId);
            model.addAttribute("consultingSessionId", consultingSessionId);
            model.addAttribute("serverUrl", serverUrl);
            model.addAttribute("invitationMode", true);
            model.addAttribute("loggedInClientId", loggedInClientId);
            
            log.info("초대링크 모드로 설정 - contractId: {}, sessionId: {}", contractId, consultingSessionId);
        } else {
            // 일반 접근 (기존 기능)
            model.addAttribute("invitationMode", false);
            log.info("일반 모드로 설정");
        }
        
        return "client/clientEntry";
    }
    
    /**
     * 고객 입장 완료 처리 (초대링크를 통한 입장)
     */
    @PostMapping("/client-entry/complete")
    public String completeEntry(
            @RequestParam String entryType,
            @RequestParam(required = false) String stampData,
            @RequestParam(required = false) String signatureData,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {
        
        Invitation invitation = (Invitation) session.getAttribute("validatedInvitation");
        Long contractId = (Long) session.getAttribute("contractId");
        String consultingSessionId = (String) session.getAttribute("consultingSessionId");
        String serverUrl = (String) session.getAttribute("serverUrl");
        Long loggedInClientId = (Long) session.getAttribute("loggedInClientId");
        
        if (invitation == null || contractId == null || consultingSessionId == null) {
            log.warn("유효하지 않은 세션으로 입장 시도 - 사용자: {}", userDetails.getUsername());
            return "redirect:/invitation/error";
        }
        
        try {
            log.info("고객 입장 완료 처리 시작 - contractId: {}, entryType: {}, 로그인 사용자: {}", 
                    contractId, entryType, userDetails.getUsername());
            
            // 1. invitation 상태를 '사용완료'로 업데이트
            invitationService.markAsUsed(invitation.getInvitationId());
            
            // 2. 상담실 입장을 위한 세션 설정
            session.setAttribute("role", "client");
            session.setAttribute("entryType", entryType);
            
            if ("stamp".equals(entryType) && stampData != null) {
                session.setAttribute("stampImage", stampData);
            } else if ("signature".equals(entryType) && signatureData != null) {
                session.setAttribute("signatureImage", signatureData);
            }
            
            log.info("고객 입장 완료 - 상담실로 이동. contractId: {}, role: client, sessionId: {}, 클라이언트: {}", 
                    contractId, consultingSessionId, userDetails.getUsername());
            
            // 3. 상담실로 이동 (동적 URL 생성)
            String contractRoomUrl = String.format("/contract/room?contractId=%d&role=client&session=%s", 
                    contractId, consultingSessionId);
            
            return "redirect:" + contractRoomUrl;
            
        } catch (Exception e) {
            log.error("고객 입장 완료 처리 실패: {}", e.getMessage(), e);
            return "redirect:/invitation/error";
        }
    }
} 