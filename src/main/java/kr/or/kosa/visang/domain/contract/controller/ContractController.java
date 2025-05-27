package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.VoiceRecord;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contract.service.VoiceRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class ContractController {

    @Autowired
    private ContractService contractService;
    
    @Autowired
    private VoiceRecordService voiceRecordService;

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
    @GetMapping("/contract/recording/{voiceId}/download")
    public ResponseEntity<Resource> downloadRecording(@PathVariable Long voiceId) {
        try {
            File file = voiceRecordService.getVoiceRecordFile(voiceId);
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("녹음 파일 다운로드 실패: voiceId={}", voiceId, e);
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
} 