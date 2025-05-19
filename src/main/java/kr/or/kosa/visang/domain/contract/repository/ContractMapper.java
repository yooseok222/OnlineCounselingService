package kr.or.kosa.visang.domain.contract.repository;

import kr.or.kosa.visang.domain.contract.model.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractMapper {
    // 모든 계약 조회
    List<Contract> selectAllContracts();
    
    List<Contract> selectMonthlyScheduleByAgentId(
            @Param("id") Long agentId,
            @Param("year") String year,
            @Param("month") String month
    );

    // 계약 조회
    Contract selectContractById(Long contractId);

    // 고객 ID로 계약 목록 조회
    List<Contract> selectContractsByClientId(Long clientId);

    // 상담사 ID로 계약 목록 조회
    List<Contract> selectContractsByAgentId(Long agentId);

    // 계약 상태별 조회
    List<Contract> selectContractsByStatus(String status);

    // 계약 추가
    int insertContract(Contract contract);

    // 계약 상태 업데이트
    int updateContractStatus(Long contractId, String status);

    // 계약 메모 업데이트
    int updateContractMemo(Long contractId, String memo);
}
