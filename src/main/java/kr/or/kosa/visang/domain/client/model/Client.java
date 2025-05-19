package kr.or.kosa.visang.domain.client.model;

import kr.or.kosa.visang.domain.user.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 고객(Client) 모델 클래스
 * User 클래스를 상속하고 고객에 특화된 필드 추가
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends User {
    private Long clientId;       // 고객 ID
    private String ssn;          // 주민등록번호 (암호화됨)
    
    /**
     * 유효성 검증 오버라이드
     * 부모 클래스의 유효성 검증 실행 후 고객 특화 필드 검증
     */
    @Override
    public Client validate() {
        super.validate();
        
        if (ssn == null || ssn.trim().isEmpty()) {
            throw new IllegalArgumentException("주민등록번호는 필수 입력 항목입니다.");
        }
        
        return this;
    }
}