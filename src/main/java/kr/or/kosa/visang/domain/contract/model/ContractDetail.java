package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class ContractDetail {
    private Long contractId;
    private String status;
    private String createdAt;
    private String contractTime;
    private String clientId;
    private String memo;

    private String contractTemplateId;
    private String contractName;

    private String clientName;
    private String clientEmail;
    private String clientPhoneNumber;
    private String clientAddress;
    private String clientProfileImageUrl;

    private String agentId;
    private String agentName;
    private String agentEmail;
    private String agentPhoneNumber;
    private String agentAddress;
    private String agentProfileImageUrl;

}