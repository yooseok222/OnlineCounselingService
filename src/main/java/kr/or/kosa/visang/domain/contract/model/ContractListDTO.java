package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class ContractListDTO {
    private Long companyId;

    private Long contractId;
    private String status;
    private String contractTime;
    private String clientId;
    private String agentId;
    private String contractTemplateId;

    // 추가 필드
    private String clientName;
    private String agentName;
    private String contractTemplateName;

    // 생성자, getter, setter 등 필요한 메서드 추가
}
