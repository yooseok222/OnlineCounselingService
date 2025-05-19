package kr.or.kosa.visang.domain.admin.model;

import kr.or.kosa.visang.domain.user.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 관리자(Admin) 모델 클래스
 * User 클래스를 상속하고 관리자에 특화된 필드 추가
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends User {
    private Long adminId;        // 관리자 ID
    private Long companyId;      // 소속 회사 ID
    
    /**
     * 유효성 검증 오버라이드
     * 부모 클래스의 유효성 검증 실행 후 관리자 특화 필드 검증
     */
    @Override
    public Admin validate() {
        super.validate();
        
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID는 필수 입력 항목입니다.");
        }
        
        return this;
    }
}