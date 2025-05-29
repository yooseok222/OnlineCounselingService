package kr.or.kosa.visang.domain.agent.repository;

import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.model.UpdateAgentDto;
import kr.or.kosa.visang.domain.contract.model.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentMapper {
    
    /**
     * 상담원 저장
     * @param agent 상담원 정보
     * @return 영향받은 행 수
     */
    int save(Agent agent);
    
    /**
     * 모든 상담원 조회
     * @return 상담원 목록
     */
    List<Agent> selectAllAgents(Long companyId);
    
    /**
     * ID로 상담원 조회
     * @param id 상담원 ID
     * @return 상담원 정보
     */
    Agent selectAgentById(Long id);
    
    /**
     * 이메일로 상담원 조회
     * @param email 이메일
     * @return 상담원 정보
     */
    Agent findByEmail(String email);
    
    /**
     * 조건으로 상담원 조회
     * @param name 이름 (옵션)
     * @param email 이메일 (옵션)
     * @param state 상태 (옵션)
     * @return 상담원 목록
     */
    List<Agent> selectAgentByCondition(@Param("name") String name, @Param("email") String email, @Param("state") String state);
    
    /**
     * 상담원 정보 업데이트
     * @param updateAgentDto 업데이트할 정보
     * @return 영향받은 행 수
     */
    int updateAgent(UpdateAgentDto updateAgentDto);
    
    /**
     * 상담원 상태 업데이트
     * @param id 상담원 ID
     * @param state 변경할 상태
     * @return 영향받은 행 수
     */
    int updateAgentStatus(@Param("id") Long id, @Param("state") String state);
    
    /**
     * 상담원 비밀번호 업데이트
     * @param agentId 상담원 ID
     * @param password 변경할 비밀번호
     * @return 영향받은 행 수
     */
    int updateAgentPassword(@Param("agentId") Long agentId, @Param("password") String password);
    
    /**
     * 이메일 중복 확인
     * @param email 이메일
     * @return 존재 여부
     */
    int isEmailExists(String email);
    
    /**
     * 전화번호 중복 확인
     */
    int isPhoneNumberExists(String phoneNumber);
    
    /**
     * 이메일 인증 상태 업데이트
     * @param email 이메일
     * @param verified 인증 여부
     * @return 영향받은 행 수
     */
    int updateEmailVerificationStatus(@Param("email") String email, @Param("verified") boolean verified);

    List<Contract> getRecentCompletedContracts(Long agentId);

    Agent findByEmailSelect(String email);

    List<Map<String, Object>> countContractByStatus(Long agentId);
}
