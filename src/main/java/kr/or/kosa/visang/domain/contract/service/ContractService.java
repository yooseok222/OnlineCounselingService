package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final AgentMapper agentMapper;
    private final ContractMapper contractMapper;

    // 모든 계약 조회
    public List<Contract> getAllContracts() {
        return contractMapper.selectAllContracts();
    }

    public List<Contract> getMonthlyScheduleByAgentId(Long agentId, String year, String month) {
        // 에이전트 ID와 연도, 월을 사용하여 월간 스케줄을 조회하는 로직을 구현합니다.
        return contractMapper.selectMonthlyScheduleByAgentId(agentId, year, month);
    }

    /*@Autowired
    private ContractMapper contractMapper;*/

    // 계약 조회
    public Contract getContractById(Long contractId) {
        return contractMapper.selectContractById(contractId);
    }

    // 고객 ID로 계약 목록 조회
    public List<Contract> getContractsByClientId(Long clientId) {
        return contractMapper.selectContractsByClientId(clientId);
    }

    // 상담사 ID로 계약 목록 조회
    public List<Contract> getContractsByAgentId(Long agentId) {
        return contractMapper.selectContractsByAgentId(agentId);
    }

    // 계약 상태별 조회
    public List<Contract> getContractsByStatus(String status) {
        return contractMapper.selectContractsByStatus(status);
    }

    // 계약 생성
    public int createContract(Contract contract) {
        // 생성일 처리
        if (contract.getCreatedAt() == null) {
            // String 타입으로 현재 시간 설정
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            contract.setCreatedAt(sdf.format(new Date()));
        }
        
        // 상태 처리
        if (contract.getStatus() == null || contract.getStatus().trim().isEmpty()) {
            contract.setStatus("완료");
        }
        
        // ID 확인 (null로 설정되어야 자동 생성됨)
        if (contract.getContractId() != null) {
            System.out.println("계약 ID 자동 생성을 위해 null로 설정: " + contract.getContractId());
            contract.setContractId(null);
        }
        
        System.out.println("계약 생성 시도: " + contract.toString());
        try {
            int result = contractMapper.insertContract(contract);
            System.out.println("계약 생성 결과: " + result + ", 생성된 계약 ID: " + contract.getContractId());
            return result;
        } catch (Exception e) {
            System.err.println("계약 생성 오류: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // 계약 상태 업데이트
    public int updateContractStatus(Long contractId, String status) {
        return contractMapper.updateContractStatus(contractId, status);
    }

    // 계약 메모 업데이트
    public int updateContractMemo(Long contractId, String memo) {
        return contractMapper.updateContractMemo(contractId, memo);
    }
}
