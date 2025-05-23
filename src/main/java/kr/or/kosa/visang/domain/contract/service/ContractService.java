package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.*;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import kr.or.kosa.visang.domain.page.model.PageRequest;
import kr.or.kosa.visang.domain.page.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final ContractMapper contractMapper;

    // 모든 계약 조회
    public List<Contract> getAllContracts() {
        return contractMapper.selectAllContracts();
    }
    // 계약관련 비즈니스 로직 구현


    //searchContracts
    public PageResult<Contract> searchContracts(ContractSearchRequest request, PageRequest pageRequest) {
        // 계약 ID, 계약 월, 상담사 ID, 고객 ID, 계약서 템플릿 이름을 사용하여 계약 목록을 조회하는 로직을 구현합니다.
        Map<String, Object> params = new HashMap<>();

        params.put("companyId", request.getCompanyId());
        params.put("contractId", request.getContractId());
        params.put("contractTime", request.getContractTime());
        params.put("agentId", request.getAgentId());
        params.put("clientId", request.getClientId());
        params.put("contractName", request.getContractName());
        params.put("status", request.getStatus());

        params.put("offset", pageRequest.getOffset());
        params.put("pageSize", pageRequest.getPageSize());

        List<Contract> contracts = contractMapper.searchContracts(params);
        int totalCount = contractMapper.countContracts(params);

        PageResult<Contract> pageResult = new PageResult<>();
        pageResult.setContent(contracts);
        pageResult.setTotalCount(totalCount);
        pageResult.setPage(pageRequest.getPage());
        pageResult.setPageSize(pageRequest.getPageSize());
        pageResult.setTotalPages(pageResult.getTotalPages());
        return pageResult;
    }

    // 최근 완료된 계약 조회
    public List<RecentCompletedContract> getRecentCompletedContract(Long companyId) {
        // 회사 ID를 사용하여 최근 완료된 계약을 조회하는 로직을 구현합니다.
        return contractMapper.selectRecentCompletedContract(companyId);
    }

    // 진행중인 계약 5건 조회
    public List<RecentCompletedContract> getRecentInProgressContract(Long companyId) {
        // 회사 ID를 사용하여 진행중인 계약을 조회하는 로직을 구현합니다.
        return contractMapper.selectInProgressContract(companyId);
    }


    public List<Contract> getMonthlyScheduleByAgentId(Long agentId, String year, String month) {
        // 에이전트 ID와 연도, 월을 사용하여 월간 스케줄을 조회하는 로직을 구현합니다.
        return contractMapper.selectMonthlyScheduleByAgentId(agentId, year, month);
    }

    // 계약 조회
    public Contract getContractById(Long contractId) {
        return contractMapper.selectContractById(contractId);
    }

    // 계약 상세 조회
    public ContractDetail getContractDetail(Long contractId) {
        ContractDetail cont = contractMapper.selectContractDetail(contractId);
        System.out.println(cont);
        return cont;
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

    // 계약 상태별 월별 숫자 조회
    public ContractStatusCountsByMonthDTO getMonthlyStatusCounts(Long companyId, int year, int month) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("year", year);
        params.put("month", month);

        return contractMapper.selectMonthlyStatusCounts(params);
    }

    //계약 월별 5개월치 완료 계약 조회
    public ContractCompleteCountsByMonthDTO getLastFiveMonthsCompleted(Long companyId, int year, int month) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("startYear", year);
        params.put("startMonth", month);
        params.put("year", year);
        params.put("month", month);

        return contractMapper.getLastFiveMonthsCompleted(params);
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
