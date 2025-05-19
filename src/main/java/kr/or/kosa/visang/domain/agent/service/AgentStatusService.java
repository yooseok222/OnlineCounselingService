package kr.or.kosa.visang.domain.agent.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.InitializingBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AgentStatusService implements InitializingBean {
    
    // 상담원 입장 상태 (상담실에 입장했는지 여부)
    private AtomicBoolean agentPresent = new AtomicBoolean(false);
    
    // 활성 상담원 수 카운터 (여러 인스턴스 지원)
    private AtomicInteger activeAgentCount = new AtomicInteger(0);
    
    @Override
    public void afterPropertiesSet() {
        // 서비스 시작 시 상담원 상태 초기화
        System.out.println("AgentStatusService 초기화: 상담원 상태 초기화");
        agentPresent.set(false);
        activeAgentCount.set(0);
    }
    
    /**
     * 상담원 입장 상태 설정
     * @param present 입장 여부
     */
    public void setAgentPresent(boolean present) {
        if (present) {
            // 입장 상태로 변경 및 카운터 증가
            agentPresent.set(true);
            activeAgentCount.incrementAndGet();
            System.out.println("상담원 입장 상태 변경: 입장 (활성 상담원: " + activeAgentCount.get() + ")");
        } else {
            // 카운터 감소
            int count = activeAgentCount.decrementAndGet();
            
            // 활성 상담원이 없으면 상태도 false로 변경
            if (count <= 0) {
                agentPresent.set(false);
                // 최소값 보정
                activeAgentCount.set(0);
                System.out.println("상담원 입장 상태 변경: 퇴장 (모든 상담원 퇴장)");
            } else {
                System.out.println("상담원 퇴장 (남은 활성 상담원: " + count + ")");
            }
        }
    }
    
    /**
     * 상담원 입장 상태 확인
     * @return 입장 여부
     */
    public boolean isAgentPresent() {
        boolean present = agentPresent.get();
        System.out.println("상담원 입장 상태 확인: " + (present ? "입장" : "미입장") + 
                " (활성 상담원: " + activeAgentCount.get() + ")");
        return present;
    }
    
    /**
     * 활성화된 상담원 수 조회
     * @return 활성 상담원 수
     */
    public int getActiveAgentCount() {
        return activeAgentCount.get();
    }
} 