package kr.or.kosa.visang.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.kosa.visang.domain.agent.service.AgentStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ConsultRoomInterceptor implements HandlerInterceptor {

    @Autowired
    private AgentStatusService agentStatusService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // URL 파라미터에서 역할 확인
        String role = request.getParameter("role");
        String contractId = request.getParameter("contractId");
        
        // 상담원인 경우 무조건 입장 허용
        if ("agent".equals(role)) {
            // 상담원이 입장했다고 상태 설정
            agentStatusService.setAgentPresent(true);
            return true;
        }
        
        // 고객인 경우 상담원 입장 상태 확인
        boolean isAgentPresent = agentStatusService.isAgentPresent();
        
        if (!isAgentPresent) {
            // 상담원이 입장하지 않았으면 대기실로 리다이렉트 (contractId 포함)
            String redirectUrl = "/waiting-room";
            if (contractId != null && !contractId.isEmpty()) {
                redirectUrl += "?contractId=" + contractId;
            }
            response.sendRedirect(redirectUrl);
            return false;
        }
        
        return true;
    }
} 