package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import kr.or.kosa.visang.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final AgentMapper agentMapper;
    private final ClientMapper clientMapper;
    private final ContractMapper contractMapper;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    // 모든 계약 조회
    public List<Contract> getAllContracts() {
        return contractMapper.selectAllContracts();
    }
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
            // String으로 현재 시간 설정
            contract.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
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

    /**
     * 상담방 생성 (상담원과 고객이 입장했을 때)
     * @param sessionId 세션 ID
     * @return 생성된 Contract 객체
     */
    @Transactional
    public Contract createConsultationRoom(String sessionId) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        String currentUserRole = SecurityUtil.getCurrentUserRole();
        
        System.out.println("=== 상담방 생성 시작 ===");
        System.out.println("세션 ID: " + sessionId);
        System.out.println("사용자 이메일: " + currentUserEmail);
        System.out.println("사용자 역할: " + currentUserRole);
        
        if (currentUserEmail == null) {
            throw new IllegalStateException("로그인된 사용자 정보를 찾을 수 없습니다.");
        }

        Contract contract = new Contract();
        
        // 현재 시간을 생성일로 설정 - String으로 직접 설정
        contract.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        // 상태를 '진행중'으로 설정
        contract.setStatus("진행중");
        
        // 사용자 역할에 따라 ID 설정 (이메일로 실제 ID 조회)
        if ("AGENT".equals(currentUserRole)) {
            // 이메일로 실제 agent ID 조회
            Agent agent = agentMapper.findByEmail(currentUserEmail);
            if (agent != null) {
                contract.setAgentId(agent.getAgentId());
                System.out.println("상담원으로 설정: " + currentUserEmail + " (ID: " + agent.getAgentId() + ")");
            } else {
                System.out.println("경고: 상담원 정보를 찾을 수 없습니다: " + currentUserEmail);
                contract.setAgentId(null);
            }
        } else if ("USER".equals(currentUserRole)) {
            // 이메일로 실제 client ID 조회
            Client client = clientMapper.findByEmail(currentUserEmail);
            if (client != null) {
                contract.setClientId(client.getClientId());
                System.out.println("고객으로 설정: " + currentUserEmail + " (ID: " + client.getClientId() + ")");
            } else {
                System.out.println("경고: 고객 정보를 찾을 수 없습니다: " + currentUserEmail);
                contract.setClientId(null);
            }
        }
        
        // 세션 ID를 메모에 임시 저장 (나중에 매칭을 위해)
        contract.setMemo("SessionId: " + sessionId);
        
        System.out.println("생성할 Contract 정보:");
        System.out.println("- Status: " + contract.getStatus());
        System.out.println("- Created At: " + contract.getCreatedAt());
        System.out.println("- Agent ID: " + contract.getAgentId());
        System.out.println("- Client ID: " + contract.getClientId());
        System.out.println("- Memo: " + contract.getMemo());
        
        try {
            int insertResult = contractMapper.insertContract(contract);
            System.out.println("DB 삽입 결과: " + insertResult);
            System.out.println("생성된 Contract ID: " + contract.getContractId());
            System.out.println("상담방 생성 완료: " + contract.toString());
            return contract;
        } catch (Exception e) {
            System.err.println("=== 상담방 생성 오류 ===");
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류 타입: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new RuntimeException("상담방 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 상담방에 사용자 추가 (고객 또는 상담원이 나중에 입장했을 때)
     * @param sessionId 세션 ID
     * @return 업데이트된 Contract 객체
     */
    @Transactional
    public Contract joinConsultationRoom(String sessionId) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        String currentUserRole = SecurityUtil.getCurrentUserRole();
        
        System.out.println("=== 상담방 참여 시작 ===");
        System.out.println("세션 ID: " + sessionId);
        System.out.println("사용자 이메일: " + currentUserEmail);
        System.out.println("사용자 역할: " + currentUserRole);
        
        if (currentUserEmail == null) {
            throw new IllegalStateException("로그인된 사용자 정보를 찾을 수 없습니다.");
        }

        // 세션 ID로 기존 계약 찾기
        System.out.println("기존 계약 조회 시도...");
        Contract existingContract = null;
        try {
            existingContract = contractMapper.selectContractBySessionId(sessionId);
            System.out.println("기존 계약 조회 결과: " + (existingContract != null ? existingContract.toString() : "null"));
        } catch (Exception e) {
            System.err.println("기존 계약 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (existingContract == null) {
            // 기존 계약이 없으면 새로 생성
            System.out.println("기존 계약이 없어 새로 생성합니다.");
            return createConsultationRoom(sessionId);
        }
        
        // 기존 계약에 사용자 정보 추가
        boolean needsUpdate = false;
        
        if ("AGENT".equals(currentUserRole) && existingContract.getAgentId() == null) {
            System.out.println("기존 계약에 상담원 정보 추가");
            // 이메일로 실제 agent ID 조회
            Agent agent = agentMapper.findByEmail(currentUserEmail);
            if (agent != null) {
                contractMapper.updateContractAgentId(existingContract.getContractId(), agent.getAgentId());
                existingContract.setAgentId(agent.getAgentId());
                System.out.println("상담원 ID 업데이트: " + agent.getAgentId());
                needsUpdate = true;
            } else {
                System.out.println("경고: 상담원 정보를 찾을 수 없습니다: " + currentUserEmail);
            }
        } else if ("USER".equals(currentUserRole) && existingContract.getClientId() == null) {
            System.out.println("기존 계약에 고객 정보 추가");
            // 이메일로 실제 client ID 조회
            Client client = clientMapper.findByEmail(currentUserEmail);
            if (client != null) {
                contractMapper.updateContractClientId(existingContract.getContractId(), client.getClientId());
                existingContract.setClientId(client.getClientId());
                System.out.println("고객 ID 업데이트: " + client.getClientId());
                needsUpdate = true;
            } else {
                System.out.println("경고: 고객 정보를 찾을 수 없습니다: " + currentUserEmail);
            }
        }
        
        // 양쪽 사용자가 모두 입장했으면 메모에서 세션 ID 제거
        if (existingContract.getAgentId() != null && existingContract.getClientId() != null) {
            System.out.println("양쪽 사용자가 모두 입장하여 메모를 정리합니다.");
            contractMapper.updateContractMemo(existingContract.getContractId(), "");
            existingContract.setMemo("");
        }
        
        try {
            System.out.println("상담방 참여 완료: " + existingContract.toString());
            return existingContract;
        } catch (Exception e) {
            System.err.println("상담방 참여 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("상담방 참여에 실패했습니다.", e);
        }
    }

    /**
     * 상담 종료 및 메모 업데이트
     * @param contractId 계약 ID
     * @param memo 상담 메모
     * @return 업데이트 결과
     */
    @Transactional
    public int endConsultation(Long contractId, String memo) {
        try {
            // 상태를 '완료'로 변경
            int statusResult = contractMapper.updateContractStatus(contractId, "완료");
            
            // 메모 업데이트
            int memoResult = contractMapper.updateContractMemo(contractId, memo);
            
            System.out.println("상담 종료 완료 - 계약 ID: " + contractId + ", 메모: " + memo);
            return statusResult + memoResult;
        } catch (Exception e) {
            System.err.println("상담 종료 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("상담 종료에 실패했습니다.", e);
        }
    }

    /**
     * PDF를 고객 이메일로 전송
     * @param contractId 계약 ID
     * @param pdfData PDF 데이터 (Base64 인코딩)
     * @param clientEmail 고객 이메일
     */
    public void sendPdfToClient(Long contractId, String pdfData, String clientEmail) {
        try {
            System.out.println("=== PDF 이메일 전송 시작 ===");
            System.out.println("계약 ID: " + contractId);
            System.out.println("고객 이메일: " + clientEmail);
            System.out.println("PDF 데이터 존재 여부: " + (pdfData != null && !pdfData.isEmpty()));
            
            if (pdfData != null && !pdfData.isEmpty()) {
                System.out.println("PDF 데이터 길이: " + pdfData.length());
                System.out.println("PDF 데이터 앞 100자: " + pdfData.substring(0, Math.min(100, pdfData.length())));
            }
            
            // 이메일 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("contractId", contractId);
            context.setVariable("currentDate", new SimpleDateFormat("yyyy년 MM월 dd일").format(new Date()));
            
            // 이메일 템플릿 처리
            String htmlContent = templateEngine.process("email/contract-completion", context);
            System.out.println("이메일 템플릿 처리 완료");
            
            // MimeMessage 생성 및 설정
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(clientEmail);
            helper.setSubject("[비상(Visang)] 상담 완료 - 계약서 전송");
            helper.setText(htmlContent, true);
            System.out.println("기본 이메일 설정 완료");
            
            // PDF 첨부 파일 추가
            if (pdfData != null && !pdfData.isEmpty()) {
                try {
                    System.out.println("PDF 첨부 파일 처리 시작");
                    
                    // Base64 데이터에서 프리픽스 제거 (data:application/pdf;base64, 또는 data:image/png;base64, 등)
                    String base64Data = pdfData;
                    if (pdfData.contains(",")) {
                        base64Data = pdfData.substring(pdfData.indexOf(",") + 1);
                        System.out.println("Base64 프리픽스 제거 완료");
                        System.out.println("프리픽스 제거 후 데이터 길이: " + base64Data.length());
                    }
                    
                    // Base64 디코딩
                    byte[] pdfBytes = java.util.Base64.getDecoder().decode(base64Data);
                    System.out.println("Base64 디코딩 완료 - 바이트 길이: " + pdfBytes.length);
                    
                    // PDF 첨부
                    helper.addAttachment("상담문서_" + contractId + ".pdf", 
                        new org.springframework.core.io.ByteArrayResource(pdfBytes));
                    System.out.println("PDF 첨부 파일 추가 완료");
                    
                } catch (IllegalArgumentException e) {
                    System.err.println("Base64 디코딩 오류: " + e.getMessage());
                    System.err.println("원본 데이터 앞 200자: " + pdfData.substring(0, Math.min(200, pdfData.length())));
                    throw new RuntimeException("PDF 데이터 형식이 올바르지 않습니다: " + e.getMessage(), e);
                } catch (Exception e) {
                    System.err.println("PDF 첨부 중 오류: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("PDF 첨부에 실패했습니다: " + e.getMessage(), e);
                }
            } else {
                System.out.println("PDF 데이터가 없어 첨부 파일 없이 전송");
            }
            
            // 이메일 전송
            System.out.println("이메일 전송 시작");
            mailSender.send(message);
            System.out.println("PDF 이메일 전송 완료 - 수신자: " + clientEmail);
            
        } catch (MessagingException e) {
            System.err.println("PDF 이메일 전송 오류 (MessagingException): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PDF 이메일 전송에 실패했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("PDF 이메일 전송 중 예상치 못한 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PDF 이메일 전송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 세션 ID로 계약 정보 조회
     * @param sessionId 세션 ID
     * @return 계약 정보
     */
    public Contract getContractBySessionId(String sessionId) {
        try {
            System.out.println("세션 ID로 계약 조회 시작: " + sessionId);
            
            // 세션 ID로 계약 정보 조회 
            Contract contract = contractMapper.selectContractBySessionId(sessionId);
            
            if (contract != null) {
                System.out.println("세션 ID로 계약 조회 성공: " + contract);
                return contract;
            } else {
                System.out.println("세션 ID에 해당하는 계약 정보 없음: " + sessionId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("세션 ID로 계약 조회 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("세션 ID로 계약 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
