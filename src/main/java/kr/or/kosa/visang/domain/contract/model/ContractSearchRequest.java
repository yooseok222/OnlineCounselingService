package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class ContractSearchRequest {
    private Long contractId; // 계약 ID
    private String contractTime; // 계약 월 (YYYY-MM 형식)
    private Long agentId; // 상담사 ID
    private Long clientId; // 고객 ID
    private String contractName; // 계약서 템플릿 이름
    private String status; // 계약 상태
}
