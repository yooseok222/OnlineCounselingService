package kr.or.kosa.visang.domain.contract.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContractSessionService {
    
    // 모든 계약 세션 데이터를 저장하는 맵
    // Key: 계약 세션 ID, Value: 세션 데이터
    private final Map<String, ContractSessionData> sessionDataMap = new ConcurrentHashMap<>();
    
    /**
     * 계약 세션 데이터 저장
     */
    public void saveSessionData(String sessionId, ContractSessionData data) {
        sessionDataMap.put(sessionId, data);
    }
    
    /**
     * 계약 세션 데이터 조회
     */
    public ContractSessionData getSessionData(String sessionId) {
        return sessionDataMap.get(sessionId);
    }
    
    /**
     * 계약 세션 데이터 삭제
     */
    public void removeSessionData(String sessionId) {
        sessionDataMap.remove(sessionId);
    }
    
    /**
     * 계약 세션 데이터 존재 여부 확인
     */
    public boolean hasSessionData(String sessionId) {
        return sessionDataMap.containsKey(sessionId);
    }
    
    /**
     * 계약 세션 데이터 클래스
     */
    public static class ContractSessionData {
        private String pdfUrl;
        private Map<Integer, Object> drawingData = new HashMap<>();
        private Map<Integer, Object> stampData = new HashMap<>();
        private Map<Integer, Object> signatureData = new HashMap<>();
        private Map<Integer, Object> textData = new HashMap<>();
        private Integer currentPage;
        
        // Getters and Setters
        public String getPdfUrl() {
            return pdfUrl;
        }
        
        public void setPdfUrl(String pdfUrl) {
            this.pdfUrl = pdfUrl;
        }
        
        public Map<Integer, Object> getDrawingData() {
            return drawingData;
        }
        
        public void setDrawingData(Map<Integer, Object> drawingData) {
            this.drawingData = drawingData;
        }
        
        public Map<Integer, Object> getStampData() {
            return stampData;
        }
        
        public void setStampData(Map<Integer, Object> stampData) {
            this.stampData = stampData;
        }
        
        public Map<Integer, Object> getSignatureData() {
            return signatureData;
        }
        
        public void setSignatureData(Map<Integer, Object> signatureData) {
            this.signatureData = signatureData;
        }
        
        public Map<Integer, Object> getTextData() {
            return textData;
        }
        
        public void setTextData(Map<Integer, Object> textData) {
            this.textData = textData;
        }
        
        public Integer getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(Integer currentPage) {
            this.currentPage = currentPage;
        }
    }
} 