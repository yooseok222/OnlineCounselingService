package kr.or.kosa.visang.domain.admin.repository;

import kr.or.kosa.visang.domain.admin.model.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    /**
     * 관리자 저장
     * @param admin 관리자 정보
     * @return 영향받은 행 수
     */
    int save(Admin admin);

    /**
     * ID로 관리자 조회
     * @param adminId 관리자 ID
     * @return 관리자 정보
     */
    Admin findById(Long adminId);

    /**
     * 이메일로 관리자 조회
     * @param email 이메일
     * @return 관리자 정보
     */
    Admin findByEmail(String email);

    /**
     * 회사 ID로 관리자 조회
     * @param companyId 회사 ID
     * @return 관리자 정보
     */
    Admin findByCompanyId(Long companyId);

    /**
     * 이메일 중복 확인
     * @param email 이메일
     * @return 존재 여부
     */
    boolean isEmailExists(String email);

    /**
     * 전화번호 중복 확인
     */
    boolean isPhoneNumberExists(String phoneNumber);

    /**
     * 이메일 인증 상태 업데이트
     * @param email 이메일
     * @param verified 인증 여부
     * @return 영향받은 행 수
     */
    int updateEmailVerificationStatus(@Param("email") String email, @Param("verified") boolean verified);

    /**
     * 비밀번호 업데이트
     * @param adminId 관리자 ID
     * @param password 암호화된 비밀번호
     * @return 영향받은 행 수
     */
    int updatePassword(@Param("adminId") Long adminId, @Param("password") String password);
}

