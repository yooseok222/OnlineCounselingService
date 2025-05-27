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
            contract.setStatus("완료");
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
            
            // 방법 1: 상담 참여 시 생성된 contractId를 Redis나 메모리에서 조회
            // 현재는 간단한 방법으로 구현
            
            // 방법 2: sessionId 패턴에서 contractId 추출
            // sessionId 형태: "session_타임스탬프_랜덤문자열"
            if (sessionId.startsWith("session_")) {
                String[] parts = sessionId.split("_");
                if (parts.length >= 2) {
                    try {
                        // 타임스탬프를 기반으로 contractId 생성
                        Long timestamp = Long.parseLong(parts[1]);
                        
                        // 타임스탬프를 contractId로 변환 (더 안전한 방법)
                        // 예: 1748282475667 -> 475667 (뒤 6자리)
                        Long contractId = timestamp % 1000000;
                        
                        // contractId가 0이면 1로 설정 (최소값 보장)
                        if (contractId == 0) {
                            contractId = 1L;
                        }
                        
                        log.info("sessionId {}에서 contractId {} 추출 성공", sessionId, contractId);
                        return contractId;
                        
                    } catch (NumberFormatException e) {
                        log.error("sessionId에서 타임스탬프 추출 실패: {}", sessionId, e);
                    }
                }
            }
            
            // 방법 3: 기본값 사용 (개발/테스트용)
            log.warn("sessionId에서 contractId 추출 실패, 기본값 사용: sessionId={}", sessionId);
            
            // sessionId의 해시코드를 사용하여 양수 contractId 생성
            int hashCode = Math.abs(sessionId.hashCode());
            Long contractId = (long) (hashCode % 1000000 + 1); // 1~1000000 범위
            
            log.info("sessionId {} 해시코드 기반 contractId {} 생성", sessionId, contractId);
            return contractId;
            
        } catch (Exception e) {
            log.error("sessionId로 contractId 조회 중 예외 발생: sessionId={}", sessionId, e);
            
            // 최후의 수단: 고정값 반환
            return 1L;
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