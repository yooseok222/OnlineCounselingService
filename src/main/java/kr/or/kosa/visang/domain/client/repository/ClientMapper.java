package kr.or.kosa.visang.domain.client.repository;

import kr.or.kosa.visang.domain.client.model.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientMapper {
    
    /**
     * 고객 저장
     * @param client 고객 정보
     * @return 영향받은 행 수
     */
    int save(Client client);
    
    /**
     * ID로 고객 조회
     * @param clientId 고객 ID
     * @return 고객 정보
     */
    Client findById(Long clientId);
    
    /**
     * 이메일로 고객 조회
     * @param email 이메일
     * @return 고객 정보
     */
    Client findByEmail(String email);
    
    /**
     * 전화번호 중복 확인
     */
    boolean isPhoneNumberExists(String phoneNumber);
    
    /**
     * 주민등록번호 중복 확인
     */
    boolean isSsnExists(String ssn);
    
    /**
     * 이메일 중복 확인
     * @param email 이메일
     * @return 존재 여부
     */
    boolean isEmailExists(String email);
    
    /**
     * 이메일 인증 상태 업데이트
     * @param email 이메일
     * @param verified 인증 여부
     * @return 영향받은 행 수
     */
    int updateEmailVerificationStatus(@Param("email") String email, @Param("verified") boolean verified);
    
    /**
     * 비밀번호 업데이트
     * @param clientId 고객 ID
     * @param password 암호화된 비밀번호
     * @return 영향받은 행 수
     */
    int updatePassword(@Param("clientId") Long clientId, @Param("password") String password);
    
    /**
     * 프로필 이미지 URL 업데이트
     * @param clientId 고객 ID
     * @param profileImageUrl 프로필 이미지 URL
     * @return 영향받은 행 수
     */
    int updateProfileImageUrl(@Param("clientId") Long clientId, @Param("profileImageUrl") String profileImageUrl);
} 