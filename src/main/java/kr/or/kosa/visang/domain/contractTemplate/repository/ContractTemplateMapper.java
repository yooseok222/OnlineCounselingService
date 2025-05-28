package kr.or.kosa.visang.domain.contractTemplate.repository;

import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ContractTemplateMapper {
    // 계약서 템플릿 관련 SQL 쿼리 메서드 정의
    Long getNextTemplateId();
    List<ContractTemplate> selectAllContractTemplates(@Param("companyId") Long companyId);
    ContractTemplate getPathAndHash(Long contractTemplateId);
    ContractTemplate selectTemplateById(Long contractTemplateId);
    void insertTemplate(ContractTemplate template);
    void updateTemplate(ContractTemplate template);
    void deleteTemplate(Long contractTemplateId);
    Optional<ContractTemplate> findByFileHash(String fileHash);

    List<ContractTemplate> selectByCompanyId(Long companyId);
}
