package kr.or.kosa.visang.domain.contract.repository;

import kr.or.kosa.visang.domain.contract.model.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractMapper {
    List<Contract> selectMonthlyScheduleByAgentId(
            @Param("id") Long agentId,
            @Param("year") String year,
            @Param("month") String month
    );
}
