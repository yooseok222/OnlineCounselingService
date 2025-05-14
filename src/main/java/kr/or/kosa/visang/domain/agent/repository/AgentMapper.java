package kr.or.kosa.visang.domain.agent.repository;

import kr.or.kosa.visang.domain.agent.model.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMapper {
    // 1) 전체 조회
    List<Agent> selectAllAgents();

    // 2) 조건 검색
    List<Agent> selectAgentByCondition(@Param("id")    Long id,
                                       @Param("email") String email,
                                       @Param("state") String state);

    // 3) 상세 조회
    Agent selectAgentById(@Param("id") Long id);


    // 4) 편집
    int updateAgent(Agent counselor);

    // 5) 상태 변경 (휴직, 퇴직, 복귀)
    int updateAgentStatus(
            @Param("id") Long id,
            @Param("status") String status
    );

    // 6) 비밀번호 변경
    int updateAgentPassword(
            @Param("id") Long id,
            @Param("password") String password
    );

}
