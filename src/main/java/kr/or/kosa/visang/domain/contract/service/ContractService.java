package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final AgentMapper agentMapper;
    private final ContractMapper contractMapper;

    // 계약관련 비즈니스 로직 구현

    public List<Contract> getContractByStatus(Long companyId, String status) {
        // 계약 상태에 따라 계약 목록을 조회하는 로직을 구현합니다.
        List<Contract> conn = contractMapper.selectContractByStatus(companyId, status);
        conn.forEach(System.out::println);
        return conn;
    }


    public List<Contract> getMonthlyScheduleByAgentId(Long agentId, String year, String month) {
        // 에이전트 ID와 연도, 월을 사용하여 월간 스케줄을 조회하는 로직을 구현합니다.
        return contractMapper.selectMonthlyScheduleByAgentId(agentId, year, month);
    }
}
