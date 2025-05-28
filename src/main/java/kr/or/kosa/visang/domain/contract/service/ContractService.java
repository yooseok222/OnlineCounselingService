package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.*;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import kr.or.kosa.visang.domain.page.model.PageRequest;
import kr.or.kosa.visang.domain.page.model.PageResult;
import kr.or.kosa.visang.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Long getPdfIdByContractId(Long contractId) {
        // 계약 ID를 사용하여 PDF ID를 조회하는 로직을 구현합니다.
        Long pdfId = contractMapper.selectPdfIdByContractId(contractId);
        if (pdfId == null)
            throw new RuntimeException("해당 계약 ID에 대한 PDF ID를 찾을 수 없습니다.");
        return pdfId;
    }

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
    
    // 계약 상세 조회 (템플릿 없이)
    public ContractDetail getContractDetailWithEmail(Long contractId) {
        try {
            ContractDetail cont = contractMapper.selectContractDetailWithEmail(contractId);
            System.out.println("Contract detail with email: " + cont);
            return cont;
        } catch (Exception e) {
            System.err.println("계약 상세 조회 (WithEmail) 오류: " + e.getMessage());
            return null;
        }
    }
    
    // 계약 ID로 고객 이메일 조회
    public String getClientEmailByContractId(Long contractId) {
        try {
            return contractMapper.selectClientEmailByContractId(contractId);
        } catch (Exception e) {
            System.err.println("고객 이메일 조회 오류: " + e.getMessage());
            return null;
        }
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
            contract.setStatus("COMPLETED");
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

        // 상태를 'IN_PROGRESS'로 설정
        contract.setStatus("IN_PROGRESS");

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
     * 상담방 참여 - agent_id와 client_id로 기존 계약 조회
     * @param sessionId 세션 ID
     * @return 기존 Contract 객체
     */
    @Transactional
    public synchronized Contract joinConsultationRoom(String sessionId) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        String currentUserRole = SecurityUtil.getCurrentUserRole();

        System.out.println("=== 상담방 참여 시작 ===");
        System.out.println("세션 ID: " + sessionId);
        System.out.println("사용자 이메일: " + currentUserEmail);
        System.out.println("사용자 역할: " + currentUserRole);

        if (currentUserEmail == null) {
            throw new IllegalStateException("로그인된 사용자 정보를 찾을 수 없습니다.");
        }

        // 현재 사용자의 ID 조회 - SecurityUtil에서 직접 가져오기
        Long currentAgentId = null;
        Long currentClientId = null;
        
        // SecurityUtil에서 직접 CustomUserDetails 가져오기
        kr.or.kosa.visang.common.config.security.CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        if (currentUser != null) {
            System.out.println("현재 사용자 정보:");
            System.out.println("- User ID: " + currentUser.getUserId());
            System.out.println("- Client ID: " + currentUser.getClientId());
            System.out.println("- Agent ID: " + currentUser.getAgentId());
            System.out.println("- Role: " + currentUser.getRole());
            System.out.println("- Name: " + currentUser.getName());
        }
        
        if ("AGENT".equals(currentUserRole)) {
            // SecurityUtil에서 직접 agentId 가져오기
            if (currentUser != null && currentUser.getAgentId() != null) {
                currentAgentId = currentUser.getAgentId();
                System.out.println("상담원 ID (SecurityUtil): " + currentAgentId);
            } else {
                // 백업: 이메일로 조회
                Agent agent = agentMapper.findByEmail(currentUserEmail);
                if (agent != null) {
                    currentAgentId = agent.getAgentId();
                    System.out.println("상담원 ID (DB 조회): " + currentAgentId);
                } else {
                    throw new IllegalStateException("상담원 정보를 찾을 수 없습니다: " + currentUserEmail);
                }
            }
        } else if ("USER".equals(currentUserRole)) {
            // SecurityUtil에서 직접 clientId 가져오기
            if (currentUser != null && currentUser.getClientId() != null) {
                currentClientId = currentUser.getClientId();
                System.out.println("고객 ID (SecurityUtil): " + currentClientId);
            } else {
                // 백업: 이메일로 조회
                Client client = clientMapper.findByEmail(currentUserEmail);
                if (client != null) {
                    currentClientId = client.getClientId();
                    System.out.println("고객 ID (DB 조회): " + currentClientId);
                } else {
                    throw new IllegalStateException("고객 정보를 찾을 수 없습니다: " + currentUserEmail);
                }
            }
        }

        // 기존 계약 조회 - 여러 방법으로 시도
        Contract existingContract = null;
        
        try {
            System.out.println("기존 계약 조회 중...");
            System.out.println("Agent ID: " + currentAgentId + ", Client ID: " + currentClientId);
            
            // 0. 먼저 세션 ID로 이미 생성된 계약이 있는지 확인
            System.out.println("세션 ID로 기존 계약 조회: " + sessionId);
            existingContract = contractMapper.selectContractBySessionId(sessionId);
            if (existingContract != null) {
                System.out.println("세션 ID로 기존 계약 발견: " + existingContract.toString());
                // 이미 진행중인 계약이면 그대로 반환
                if ("IN_PROGRESS".equals(existingContract.getStatus())) {
                    System.out.println("이미 진행중인 계약입니다.");
                    return existingContract;
                }
                // PENDING 상태면 진행중으로 변경 후 반환
                if ("PENDING".equals(existingContract.getStatus())) {
                    contractMapper.updateContractStatus(existingContract.getContractId(), "IN_PROGRESS");
                    existingContract.setStatus("IN_PROGRESS");
                    System.out.println("PENDING 계약을 IN_PROGRESS로 변경: " + existingContract.getContractId());
                    return existingContract;
                }
            }
            
            // 1. agent_id와 client_id가 모두 있는 경우 - 정확한 매칭
            if (currentAgentId != null && currentClientId != null) {
                System.out.println("Agent ID와 Client ID로 기존 PENDING 계약 조회");
                existingContract = contractMapper.selectContractByAgentAndClient(currentAgentId, currentClientId);
                System.out.println("Agent+Client 조회 결과: " + (existingContract != null ? existingContract.toString() : "null"));
            }
            
            // 2. 위에서 찾지 못했고 상담원인 경우 - 상담원의 PENDING 계약 조회
            if (existingContract == null && currentAgentId != null) {
                System.out.println("상담원의 PENDING 계약 조회");
                List<Contract> agentContracts = contractMapper.selectContractsByAgentId(currentAgentId);
                if (agentContracts != null) {
                    for (Contract contract : agentContracts) {
                        if ("PENDING".equals(contract.getStatus())) {
                            existingContract = contract;
                            System.out.println("상담원 PENDING 계약 발견: " + contract.toString());
                            break;
                        }
                    }
                }
            }
            
            // 3. 위에서 찾지 못했고 고객인 경우 - 고객의 PENDING 계약 조회
            if (existingContract == null && currentClientId != null) {
                System.out.println("고객의 PENDING 계약 조회");
                List<Contract> clientContracts = contractMapper.selectContractsByClientId(currentClientId);
                if (clientContracts != null) {
                    for (Contract contract : clientContracts) {
                        if ("PENDING".equals(contract.getStatus())) {
                            existingContract = contract;
                            System.out.println("고객 PENDING 계약 발견: " + contract.toString());
                            break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("기존 계약 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }

        if (existingContract == null) {
            // 기존 계약이 없으면 새로 생성
            System.out.println("기존 PENDING 계약이 없어 새로 생성합니다.");
            return createConsultationRoom(sessionId);
        }

        // 기존 계약의 상태를 'IN_PROGRESS'로 변경
        try {
            contractMapper.updateContractStatus(existingContract.getContractId(), "IN_PROGRESS");
            existingContract.setStatus("IN_PROGRESS");
            
            // 세션 ID를 메모에 추가 (상담 종료 시 필요)
            String updatedMemo = existingContract.getMemo() + " [SessionId: " + sessionId + "]";
            contractMapper.updateContractMemo(existingContract.getContractId(), updatedMemo);
            existingContract.setMemo(updatedMemo);
            
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
            System.out.println("=== 상담 종료 시작 ===");
            System.out.println("계약 ID: " + contractId);
            System.out.println("메모: " + memo);
            
            // 계약 상태를 'COMPLETED'로 변경하고 메모 업데이트
            int result = contractMapper.endConsultation(contractId, memo);
            
            if (result > 0) {
                System.out.println("상담 종료 성공");
            } else {
                System.out.println("상담 종료 실패 - 업데이트된 행이 없음");
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("상담 종료 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("상담 종료에 실패했습니다.", e);
        }
    }

    /**
     * 상담 종료 후 자동으로 PDF 메일 발송
     * @param contractId 계약 ID
     */
    private void sendPdfEmailAfterConsultation(Long contractId) {
        try {
            System.out.println("=== 상담 종료 후 자동 PDF 메일 발송 시작 ===");
            System.out.println("계약 ID: " + contractId);
            
            // 계약 상세 정보 조회 (고객 이메일 포함)
            ContractDetail contractDetail = getContractDetail(contractId);
            
            if (contractDetail == null) {
                System.err.println("계약 정보를 찾을 수 없습니다. 계약 ID: " + contractId);
                return;
            }
            
            String clientEmail = contractDetail.getClientEmail();
            if (clientEmail == null || clientEmail.trim().isEmpty()) {
                System.err.println("고객 이메일이 없습니다. 계약 ID: " + contractId);
                return;
            }
            
            System.out.println("고객 이메일: " + clientEmail);
            
            // PDF 데이터 없이 메일 발송 (상담 완료 알림)
            // 실제 PDF 파일은 프론트엔드에서 생성되므로, 여기서는 상담 완료 알림만 발송
            sendPdfToClient(contractId, null, clientEmail);
            
            System.out.println("상담 종료 후 자동 PDF 메일 발송 완료");
            
        } catch (Exception e) {
            System.err.println("상담 종료 후 자동 PDF 메일 발송 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("상담 종료 후 PDF 메일 발송에 실패했습니다: " + e.getMessage(), e);
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
            System.out.println("=== 세션 ID로 계약 조회 ===");
            System.out.println("세션 ID: " + sessionId);
            
            // 메모에서 세션 ID를 포함하는 계약 조회
            Contract contract = contractMapper.selectContractBySessionId(sessionId);
            
            if (contract != null) {
                System.out.println("계약 조회 성공: " + contract.toString());
            } else {
                System.out.println("해당 세션 ID의 계약을 찾을 수 없습니다.");
            }
            
            return contract;
        } catch (Exception e) {
            System.err.println("세션 ID로 계약 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 고객의 오늘 계약 조회
     * @param clientId 고객 ID
     * @return 오늘의 계약 목록
     */
    public List<Contract> getTodayContractsByClientId(Long clientId) {
        // 오늘 날짜의 계약만 조회
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", clientId);
        params.put("contractDate", today);
        params.put("status", "PENDING"); // 예약된 상태만
        
        return contractMapper.selectTodayContractsByClientId(params);
    }
    
    /**
     * 고객별 계약 상태 카운트 조회
     * @param clientId 고객 ID
     * @return 상태별 계약 수
     */
    public Map<String, Integer> getContractCountsByClientId(Long clientId) {
        Map<String, Integer> rawCounts = contractMapper.selectContractCountsByClientId(clientId);
        
        // 대소문자 문제를 해결하기 위해 명시적으로 키를 매핑
        Map<String, Integer> counts = new HashMap<>();
        counts.put("pending", getIntValueFromMap(rawCounts, "pending", "PENDING"));
        counts.put("inProgress", getIntValueFromMap(rawCounts, "inProgress", "INPROGRESS"));
        counts.put("completed", getIntValueFromMap(rawCounts, "completed", "COMPLETED"));
        counts.put("canceled", getIntValueFromMap(rawCounts, "canceled", "CANCELED"));
        counts.put("total", getIntValueFromMap(rawCounts, "total", "TOTAL"));
        
        return counts;
    }
    
    private Integer getIntValueFromMap(Map<String, Integer> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof java.math.BigDecimal) {
                    return ((java.math.BigDecimal) value).intValue();
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
        }
        return 0;
    }
    
    /**
     * 고객별 계약 목록 페이징 조회
     * @param clientId 고객 ID
     * @param status 계약 상태 (선택사항)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 계약 목록
     */
    public Map<String, Object> getContractsByClientIdPaged(Long clientId, String status, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", clientId);
        params.put("status", status);
        params.put("offset", (page - 1) * size);
        params.put("pageSize", size);
        
        List<Contract> contracts = contractMapper.selectContractsByClientIdPaged(params);
        int totalCount = contractMapper.countContractsByClientId(params);
        
        Map<String, Object> result = new HashMap<>();
        result.put("contracts", contracts);
        result.put("totalCount", totalCount);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));
        
        return result;
    }
    
    /**
     * 계약 취소
     * @param contractId 계약 ID
     * @return 업데이트된 행 수
     */
    @Transactional
    public int cancelContract(Long contractId) {
        return contractMapper.updateContractStatus(contractId, "CANCELED");
    }
}
