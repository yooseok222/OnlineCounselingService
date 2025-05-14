package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final AgentMapper agentMapper;
    private final ContractMapper contractMapper;


    public void getMonthlyScheduleByAgentId(Long agentId, String year, String month) {
        // 에이전트 ID와 연도, 월을 사용하여 월간 스케줄을 조회하는 로직을 구현합니다.
        contractMapper.selectMonthlyScheduleByAgentId(agentId, year, month).forEach(System.out::println);
    }
}
