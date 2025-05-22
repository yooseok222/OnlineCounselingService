package kr.or.kosa.visang.domain.contract.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import kr.or.kosa.visang.domain.contract.model.ContractDetail;
import kr.or.kosa.visang.domain.contract.model.ContractSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.Schedule;

@Mapper
public interface ContractMapper {
    // 모든 계약 조회
    List<Contract> selectAllContracts();

    List<Contract> selectMonthlyScheduleByAgentId(
            @Param("id") Long agentId,
            @Param("year") String year,
            @Param("month") String month
    );

    List<Contract> searchContracts(Map<String, Object> params);

    int countContracts(Map<String, Object> params);


    // 계약 조회
    Contract selectContractById(Long contractId);

    // 계약 상세 조회
    ContractDetail selectContractDetail(Long contractId);

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


	void insertSchedule(Contract c);

	List<Schedule> selectSchedulesByAgent(@Param("agentId") Long agentId);

	int countByClientAndTime(@Param("clientId") Long clientId, @Param("contractTime") LocalDateTime contractTime);

	int countByAgentAndTime(@Param("agentId") Long agentId, @Param("contractTime") LocalDateTime contractTime);

	// excludeContractId : 자기 자신 제외
	int countByClientAndTimeExcept(@Param("clientId") Long clientId, @Param("contractTime") LocalDateTime contractTime,
			@Param("excludeContractId") Long excludeContractId);

	int countByAgentAndTimeExcept(@Param("agentId") Long agentId, @Param("contractTime") LocalDateTime contractTime,
			@Param("excludeContractId") Long excludeContractId);

	void updateSchedule(Schedule dto);

	int deleteSchedule(@Param("contractId") Long contractId);

	List<Schedule> findTodayContracts(Map<String, Object> params);

	List<Schedule> selectSchedulesByAgentAndDateRange(Map<String, Object> param);

	Contract findById(@Param("contractId") Long contractId);

}
