package kr.or.kosa.visang.domain.user.repository;

import kr.or.kosa.visang.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자 정보 데이터 접근을 위한 매퍼 인터페이스
 */
@Mapper
public interface UserMapper {

    /**
     * 이메일로 사용자 유형 조회
     * 
     * @param email 이메일
     * @return 사용자 유형 (CLIENT, AGENT, ADMIN)
     */
    String findUserTypeByEmail(@Param("email") String email);
    
    /**
     * 이메일로 사용자 조회
     * 
     * @param email 이메일
     * @return 사용자 정보
     */
    User findByEmail(@Param("email") String email);
    
    /**
     * 이메일 중복 확인
     * 
     * @param email 이메일
     * @return 사용자 수 (0: 중복 없음, 1 이상: 중복)
     */
    int countByEmail(@Param("email") String email);
    
    /**
     * 전화번호 중복 확인
     * 
     * @param phoneNumber 전화번호
     * @return 사용자 수 (0: 중복 없음, 1 이상: 중복)
     */
    int countByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    /**
     * 인증 코드로 사용자 조회
     * 
     * @param verificationCode 인증 코드
     * @return 사용자 정보
     */
    User findByVerificationCode(@Param("verificationCode") String verificationCode);
    
    /**
     * 사용자 이메일 인증 상태 업데이트
     * 
     * @param userId 사용자 ID
     * @param verified 인증 여부
     * @return 영향받은 행 수
     */
    int updateEmailVerificationStatus(@Param("userId") Long userId, @Param("verified") boolean verified);
} 