package kr.or.kosa.visang.domain.contract.model;

/**
 * 고객이 상담원에게 PDF 요청 시 사용하는 메시지
 */
public class RequestPdfMessage {
    private String sessionId;
    private String requesterId;

    public RequestPdfMessage() {
    }

    public RequestPdfMessage(String sessionId, String requesterId) {
        this.sessionId = sessionId;
        this.requesterId = requesterId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
} 