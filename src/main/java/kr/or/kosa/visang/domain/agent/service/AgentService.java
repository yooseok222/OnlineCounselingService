package kr.or.kosa.visang.domain.agent.service;

import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    // 에이전트 목록 조회
    public List<Agent> getAgentList() {
        return agentMapper.selectAllAgents();
    }

    // 에이전트 추가
    public void addAgent() {
        System.out.println("addAgent() called");
    }

    //조건 검색
    public List<Agent> searchAgent(String name, String email, String state) {
        return agentMapper.selectAgentByCondition(name, email, state);
    }

    // 상세정보 조회
    public Agent getAgentInfo(Long id) {
        return agentMapper.selectAgentById(id);
    }

    // 에이전트 상세정보
    public void getAgentDetail(Long id) {
        agentMapper.selectAgentById(id);
    }

    // 에이전트 수정
    public void updateAgent(Agent agent) {
        // 에이전트를 수정하는 로직을 구현합니다.
        agentMapper.updateAgent(agent);
        System.out.println("updateAgent() called");
    }

    // 에이전트 상태 변경
    public void updateAgentStatus(Long id, String state) {
        agentMapper.updateAgentStatus(id, state);
    }

    // 에이전트 비밀번호 변경
    public void updateAgentPassword(Long id, String password) {
        agentMapper.updateAgentPassword(id, password);
    }

}
