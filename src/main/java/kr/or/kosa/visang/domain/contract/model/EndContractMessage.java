package kr.or.kosa.visang.domain.contract.model;

public class EndContractMessage {
    private String message;
    private Long contractId;
    private String redirectUrl;

    public EndContractMessage() {
    }

    public EndContractMessage(String message) {
        this.message = message;
    }
    
    public EndContractMessage(String message, Long contractId) {
        this.message = message;
        this.contractId = contractId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getContractId() {
        return contractId;
    }
    
    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
} 