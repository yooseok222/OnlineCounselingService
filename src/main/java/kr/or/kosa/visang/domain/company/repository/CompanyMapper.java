package kr.or.kosa.visang.domain.company.repository;

import kr.or.kosa.visang.domain.company.model.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompanyMapper {
    
    /**
     * 회사 저장
     * @param company 회사 정보
     * @return 영향받은 행 수
     */
    int save(Company company);
    
    /**
     * ID로 회사 조회
     * @param companyId 회사 ID
     * @return 회사 정보
     */
    Company findById(Long companyId);
    
    /**
     * 회사명으로 회사 조회
     * @param companyName 회사명
     * @return 회사 정보
     */
    Company findByName(String companyName);
    
    /**
     * 회사명 중복 확인
     * @param companyName 회사명
     * @return 존재 여부
     */
    boolean isCompanyNameExists(String companyName);
} 