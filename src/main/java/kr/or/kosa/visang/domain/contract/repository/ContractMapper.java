package kr.or.kosa.visang.domain.contract.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.Schedule;

@Mapper
public interface ContractMapper {
    List<Contract> selectContractByStatus(
            @Param("companyId") Long companyId,
            @Param("status")String status
    );

    List<Contract> selectMonthlyScheduleByAgentId(
            @Param("id") Long agentId,
            @Param("year") String year,
            @Param("month") String month
    );
    

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
