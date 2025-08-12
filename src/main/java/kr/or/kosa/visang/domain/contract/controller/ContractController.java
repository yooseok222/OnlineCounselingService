package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.VoiceRecord;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contract.service.VoiceRecordService;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.service.ContractTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api")
public class ContractController {

    @Autowired
    private ContractService contractService;
    
    @Autowired
    private VoiceRecordService voiceRecordService;
    
    @Autowired
    private ContractTemplateService contractTemplateService;
    
    // 템플릿 파일 업로드 경로 (application.properties에서 설정 가능)
    @Value("${contract.template.upload.dir:#{null}}")
    private String templateUploadDir;

    /**
     * 계약 생성 API
     */
    @PostMapping("/contract")
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody Contract contract) {
        // 디버깅 로그
        System.out.println("계약 생성 요청 수신 (ContractController): " + contract);
        
        // 계약 ID 설정 제거 (데이터베이스에서 자동 생성되도록)
        contract.setContractId(null);
        
        // 상태가 비어있거나 null인 경우 기본값 설정
        if (contract.getStatus() == null || contract.getStatus().trim().isEmpty()) {
            contract.setStatus("COMPLETED");
        }
        
        // 실제 저장 시도
        int result = 0;
        try {
            result = contractService.createContract(contract);
        } catch (Exception e) {
            System.err.println("계약 저장 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("success", result > 0);
        response.put("message", result > 0 ? "계약이 생성되었습니다." : "계약 생성에 실패했습니다.");
        response.put("contractId", contract.getContractId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 계약 템플릿 정보 API
     * 계약 ID에 해당하는 템플릿 정보와 PDF URL을 반환
     */
    @GetMapping("/contract/{contractId}/template")
    public ResponseEntity<Map<String, Object>> getContractTemplate(@PathVariable Long contractId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("계약 템플릿 정보 요청: contractId={}", contractId);
            
            // 계약 정보 조회
            Contract contract = contractService.getContractById(contractId);
            if (contract == null) {
                log.warn("계약 정보를 찾을 수 없음: contractId={}", contractId);
                response.put("success", false);
                response.put("message", "계약 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 계약에 연결된 템플릿 ID 조회
            Long templateId = contract.getContractTemplateId();
            if (templateId == null) {
                log.warn("계약에 연결된 템플릿이 없음: contractId={}", contractId);
                response.put("success", false);
                response.put("message", "계약에 연결된 템플릿이 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 템플릿 정보 조회
            ContractTemplate template = contractTemplateService.getTemplateById(templateId);
            if (template == null) {
                log.warn("템플릿 정보를 찾을 수 없음: templateId={}", templateId);
                response.put("success", false);
                response.put("message", "템플릿 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 템플릿 PDF URL 생성 - 직접 이 컨트롤러에서 제공하는 엔드포인트로 변경
            String templateUrl = "/api/contract/" + contractId + "/template-pdf";
            
            response.put("success", true);
            response.put("templateId", templateId);
            response.put("templateName", template.getContractName());
            response.put("templateUrl", templateUrl);
            
            log.info("계약 템플릿 정보 응답: templateId={}, templateName={}", templateId, template.getContractName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계약 템플릿 정보 조회 실패: contractId={}", contractId, e);
            response.put("success", false);
            response.put("message", "템플릿 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 계약 템플릿 PDF 파일 직접 제공 API
     * 계약 ID를 통해 연결된 템플릿의 PDF 파일을 직접 제공
     */
    @GetMapping("/contract/{contractId}/template-pdf")
    public ResponseEntity<?> getContractTemplatePdfDirectly(@PathVariable Long contractId) {
        try {
            log.info("계약 템플릿 PDF 직접 요청: contractId={}", contractId);
            
            // 계약 정보 조회
            Contract contract = contractService.getContractById(contractId);
            if (contract == null) {
                log.warn("계약 정보를 찾을 수 없음: contractId={}", contractId);
                return createDefaultPdfResponse("계약 정보를 찾을 수 없습니다.");
            }
            
            // 계약에 연결된 템플릿 ID 조회
            Long templateId = contract.getContractTemplateId();
            if (templateId == null) {
                log.warn("계약에 연결된 템플릿이 없음 - 기본 템플릿 제공: contractId={}", contractId);
                return createDefaultPdfResponse("이 계약에는 연결된 템플릿이 없습니다. 기본 템플릿을 제공합니다.");
            }
            
            // 템플릿 PDF 데이터 가져오기 시도
            try {
                Resource pdfResource = contractTemplateService.getTemplateResource(templateId);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template_" + templateId + ".pdf\"")
                        .body(pdfResource);
            } catch (Exception e) {
                log.warn("템플릿 PDF 리소스 가져오기 실패, 기본 PDF 생성: {}", e.getMessage());
                return createDefaultPdfResponse("템플릿 PDF를 가져오는 데 실패했습니다. 기본 템플릿을 제공합니다.");
            }
        } catch (Exception e) {
            log.error("템플릿 PDF 제공 실패: contractId={}", contractId, e);
            return createDefaultPdfResponse("템플릿 PDF 제공에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 기본 PDF 응답 생성
     */
    private ResponseEntity<?> createDefaultPdfResponse(String message) {
        log.info("기본 PDF 응답 생성: {}", message);
        try {
            // 임시 파일 생성
            Path tempFile = Files.createTempFile("default_template_", ".txt");
            Files.write(tempFile, message.getBytes());
            
            // 리소스로 변환
            Resource resource = new FileSystemResource(tempFile.toFile());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"default_template.txt\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("기본 PDF 생성 실패", e);
            // 메모리 리소스로 대체
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message);
        }
    }

    /**
     * 세션 정보 조회 API
     * 세션 ID에 해당하는 계약 ID를 반환
     */
    @GetMapping("/session/{sessionId}/info")
    public ResponseEntity<Map<String, Object>> getSessionInfo(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("세션 정보 조회 요청: sessionId={}", sessionId);
            
            // 세션 ID로 계약 정보 조회
            Contract contract = contractService.getContractBySessionId(sessionId);
            if (contract == null) {
                log.warn("세션 ID로 계약 정보를 찾을 수 없음: sessionId={}", sessionId);
                response.put("success", false);
                response.put("message", "세션 ID로 계약 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("contractId", contract.getContractId());
            response.put("agentId", contract.getAgentId());
            response.put("clientId", contract.getClientId());
            response.put("status", contract.getStatus());
            
            log.info("세션 정보 응답: sessionId={}, contractId={}", sessionId, contract.getContractId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("세션 정보 조회 실패: sessionId={}", sessionId, e);
            response.put("success", false);
            response.put("message", "세션 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 세션 참가자 상태 조회 API
     * 세션에 상담원과 고객이 모두 입장했는지 여부를 반환
     */
    @GetMapping("/session/{sessionId}/participants")
    public ResponseEntity<Map<String, Object>> getSessionParticipantsStatus(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("세션 참가자 상태 조회 요청: sessionId={}", sessionId);
            
            // 여기서는 양쪽 참가자가 입장했는지 확인하는 로직이 필요
            // 실제 구현에서는 WebSocket 세션 관리나 데이터베이스를 통해 확인해야 함
            // 테스트를 위해 임시로 항상 true 반환
            boolean agentJoined = true;
            boolean clientJoined = true;
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("agentJoined", agentJoined);
            response.put("clientJoined", clientJoined);
            response.put("allJoined", agentJoined && clientJoined);
            
            log.info("세션 참가자 상태 응답: sessionId={}, agentJoined={}, clientJoined={}", 
                    sessionId, agentJoined, clientJoined);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("세션 참가자 상태 조회 실패: sessionId={}", sessionId, e);
            response.put("success", false);
            response.put("message", "세션 참가자 상태 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 녹음 파일 업로드 API
     */
    @PostMapping("/contract/upload-recording")
    public ResponseEntity<Map<String, Object>> uploadRecording(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        
        log.info("녹음 파일 업로드 요청: sessionId={}, fileName={}, fileSize={}", 
                sessionId, file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 파일 검증
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "업로드할 파일이 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 파일 크기 검증 (최대 50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "파일 크기가 너무 큽니다. (최대 50MB)");
                return ResponseEntity.badRequest().body(response);
            }
            
            // sessionId로 contractId 조회
            Long contractId = getContractIdBySessionId(sessionId);
            if (contractId == null) {
                log.error("sessionId로 contractId를 찾을 수 없습니다: {}", sessionId);
                response.put("success", false);
                response.put("message", "해당 세션의 상담 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("sessionId {}로 contractId {} 조회 성공", sessionId, contractId);
            
            // 녹음 파일 저장
            VoiceRecord voiceRecord = voiceRecordService.saveVoiceRecord(file, contractId);
            
            response.put("success", true);
            response.put("message", "녹음 파일이 성공적으로 업로드되었습니다.");
            response.put("voiceId", voiceRecord.getVoiceId());
            response.put("filePath", voiceRecord.getFilePath());
            response.put("contractId", contractId);
            
            log.info("녹음 파일 업로드 성공: voiceId={}, contractId={}", voiceRecord.getVoiceId(), contractId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("녹음 파일 업로드 실패: sessionId={}", sessionId, e);
            response.put("success", false);
            response.put("message", "녹음 파일 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * sessionId로 contractId 조회하는 헬퍼 메서드
     */
    private Long getContractIdBySessionId(String sessionId) {
        try {
            log.info("sessionId로 contractId 조회 시도: {}", sessionId);
            
            // ContractService를 통해 실제 계약 조회
            Contract contract = contractService.getContractBySessionId(sessionId);
            
            if (contract != null) {
                log.info("sessionId {}로 contractId {} 조회 성공", sessionId, contract.getContractId());
                return contract.getContractId();
            } else {
                log.warn("sessionId {}에 해당하는 계약을 찾을 수 없습니다", sessionId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("sessionId로 contractId 조회 중 예외 발생: sessionId={}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 계약별 녹음 파일 목록 조회 API
     */
    @GetMapping("/contract/{contractId}/recordings")
    public ResponseEntity<Map<String, Object>> getRecordings(@PathVariable Long contractId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<VoiceRecord> recordings = voiceRecordService.getVoiceRecordsByContractId(contractId);
            
            response.put("success", true);
            response.put("recordings", recordings);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("녹음 파일 목록 조회 실패: contractId={}", contractId, e);
            response.put("success", false);
            response.put("message", "녹음 파일 목록 조회에 실패했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 녹음 파일 다운로드 API
     */
    @GetMapping("/contract/recording/{contractId}/download")
    public ResponseEntity<Resource> downloadRecording(@PathVariable Long contractId) {
        try {
            Long voiceId = voiceRecordService.getVoiceRecordByContractId(contractId);

            File file = voiceRecordService.getVoiceRecordFile(voiceId);
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("녹음 파일 다운로드 실패: voiceId={}", contractId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 녹음 파일 삭제 API
     */
    @DeleteMapping("/contract/recording/{voiceId}")
    public ResponseEntity<Map<String, Object>> deleteRecording(@PathVariable Long voiceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            voiceRecordService.deleteVoiceRecord(voiceId);
            
            response.put("success", true);
            response.put("message", "녹음 파일이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("녹음 파일 삭제 실패: voiceId={}", voiceId, e);
            response.put("success", false);
            response.put("message", "녹음 파일 삭제에 실패했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 계약 템플릿 PDF 파일 제공 API
     */
    @GetMapping("/contract-templates/{templateId}/pdf")
    public ResponseEntity<Resource> getContractTemplatePdf(@PathVariable Long templateId) {
        try {
            log.info("계약 템플릿 PDF 요청: templateId={}", templateId);
            
            // 템플릿 정보 조회
            ContractTemplate template = contractTemplateService.getTemplateById(templateId);
            if (template == null) {
                log.warn("템플릿 정보를 찾을 수 없음: templateId={}", templateId);
                return ResponseEntity.notFound().build();
            }
            
            // 템플릿 PDF 파일 경로 확인 (템플릿 경로는 파일명 또는 상대 경로로 가정)
            String pdfFilePath = null;
            
            // 1. 템플릿 서비스에서 PDF 경로를 가져오는 메서드가 있는 경우
            try {
                pdfFilePath = contractTemplateService.getTemplatePdfPath(templateId);
                log.info("템플릿 서비스에서 PDF 경로 조회: {}", pdfFilePath);
            } catch (Exception e) {
                log.warn("템플릿 서비스에서 PDF 경로를 가져오는 데 실패: {}", e.getMessage());
            }
            
            // 2. 템플릿 객체에서 직접 경로 가져오기 시도 (대체 방법)
            if (pdfFilePath == null) {
                try {
                    // 여러 가능한 메서드명 시도
                    Method getPdfMethod = template.getClass().getMethod("getPdfPath");
                    pdfFilePath = (String) getPdfMethod.invoke(template);
                    log.info("템플릿 객체의 getPdfPath()에서 경로 조회: {}", pdfFilePath);
                } catch (Exception e1) {
                    try {
                        Method getFilePathMethod = template.getClass().getMethod("getFilePath");
                        pdfFilePath = (String) getFilePathMethod.invoke(template);
                        log.info("템플릿 객체의 getFilePath()에서 경로 조회: {}", pdfFilePath);
                    } catch (Exception e2) {
                        try {
                            Method getFilePathMethod = template.getClass().getMethod("getTemplatePath");
                            pdfFilePath = (String) getFilePathMethod.invoke(template);
                            log.info("템플릿 객체의 getTemplatePath()에서 경로 조회: {}", pdfFilePath);
                        } catch (Exception e3) {
                            log.warn("템플릿 객체에서 경로를 가져오는 데 실패: {}", e3.getMessage());
                        }
                    }
                }
            }
            
            // 3. 경로가 없으면 기본 형식으로 구성 (템플릿 ID 기반)
            if (pdfFilePath == null) {
                pdfFilePath = "template_" + templateId + ".pdf";
                log.info("기본 형식으로 경로 구성: {}", pdfFilePath);
            }
            
            // 파일 객체 생성
            File pdfFile = null;
            
            // 업로드 디렉토리 설정이 있는 경우
            if (templateUploadDir != null && !templateUploadDir.isEmpty()) {
                pdfFile = new File(templateUploadDir, pdfFilePath);
                log.info("설정된 업로드 디렉토리 사용: {}", pdfFile.getAbsolutePath());
            } 
            // 상대 경로인 경우 (시스템 속성에서 임시 디렉토리 사용)
            else if (!pdfFilePath.startsWith("/")) {
                String tempDir = System.getProperty("java.io.tmpdir");
                pdfFile = new File(tempDir, "contract_templates/" + pdfFilePath);
                log.info("임시 디렉토리 사용: {}", pdfFile.getAbsolutePath());
            }
            // 절대 경로인 경우
            else {
                pdfFile = new File(pdfFilePath);
                log.info("절대 경로 사용: {}", pdfFile.getAbsolutePath());
            }
            
            // 파일 존재 확인
            if (!pdfFile.exists() || !pdfFile.isFile()) {
                log.warn("템플릿 PDF 파일이 존재하지 않음: {}", pdfFile.getAbsolutePath());
                
                // 임시로 빈 PDF 생성 (개발용, 실제 운영에서는 제거)
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(new ByteArrayResource("임시 PDF 콘텐츠".getBytes()));
            }
            
            Resource resource = new FileSystemResource(pdfFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfFile.getName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("템플릿 PDF 제공 실패: templateId={}", templateId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 