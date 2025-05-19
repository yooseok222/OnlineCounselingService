package kr.or.kosa.visang.domain.agent.model;

import kr.or.kosa.visang.domain.user.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 상담원(Agent) 모델 클래스
 * User 클래스를 상속하고 상담원에 특화된 필드 추가
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Agent extends User {
    private Long agentId;        // 상담원 ID
    private Long companyId;      // 소속 회사 ID
    private String state;        // 상태 (ACTIVE, INACTIVE)
    
    /**
     * 유효성 검증 오버라이드
     * 부모 클래스의 유효성 검증 실행 후 상담원 특화 필드 검증
     */
    @Override
    public Agent validate() {
        super.validate();
        
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID는 필수 입력 항목입니다.");
        }
        
        if (state == null || state.trim().isEmpty()) {
            state = "INACTIVE"; // 기본값 설정
        }
        
        return this;
    }
}
