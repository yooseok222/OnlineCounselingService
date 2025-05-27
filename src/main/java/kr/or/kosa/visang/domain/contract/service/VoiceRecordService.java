package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.VoiceRecord;
import kr.or.kosa.visang.domain.contract.repository.VoiceRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceRecordService {

    private final VoiceRecordMapper voiceRecordMapper;

    @Value("${file.upload-dir.voice-record:upload/voice_record}")
    private String uploadDir;

    /**
     * 녹음 파일 업로드 및 DB 저장
     */
    public VoiceRecord saveVoiceRecord(MultipartFile file, Long contractId) {
        try {
            // 업로드 디렉토리 생성 (절대 경로 사용)
            Path uploadPath;
            if (Paths.get(uploadDir).isAbsolute()) {
                uploadPath = Paths.get(uploadDir);
            } else {
                // 상대 경로인 경우 프로젝트 루트 기준으로 절대 경로 생성
                uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir);
            }
            
            log.info("녹음 파일 저장 경로: {}", uploadPath.toString());
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("녹음 파일 디렉토리 생성 완료: {}", uploadPath.toString());
            }

            // 파일명 생성 (UUID + 타임스탬프)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new java.util.Date());
            String fileName = String.format("voice_%s_%s_%s.webm", 
                contractId, timestamp, UUID.randomUUID().toString().substring(0, 8));
            
            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            log.info("녹음 파일 저장 완료: {}", filePath.toString());

            // DB에 저장
            VoiceRecord voiceRecord = VoiceRecord.builder()
                    .filePath(filePath.toString())
                    .createdAt(new java.util.Date())
                    .contractId(contractId)
                    .build();

            voiceRecordMapper.insertVoiceRecord(voiceRecord);
            
            log.info("녹음 파일 DB 저장 완료: voiceId={}, contractId={}", voiceRecord.getVoiceId(), contractId);
            
            return voiceRecord;

        } catch (IOException e) {
            log.error("녹음 파일 저장 실패: contractId={}", contractId, e);
            throw new RuntimeException("녹음 파일 저장에 실패했습니다.", e);
        }
    }

    /**
     * 계약 ID로 녹음 파일 목록 조회
     */
    public List<VoiceRecord> getVoiceRecordsByContractId(Long contractId) {
        return voiceRecordMapper.findByContractId(contractId);
    }

    /**
     * 녹음 파일 조회
     */
    public VoiceRecord getVoiceRecord(Long voiceId) {
        return voiceRecordMapper.findById(voiceId);
    }

    /**
     * 녹음 파일 삭제
     */
    public void deleteVoiceRecord(Long voiceId) {
        VoiceRecord voiceRecord = voiceRecordMapper.findById(voiceId);
        if (voiceRecord != null) {
            // 파일 삭제
            try {
                Path filePath = Paths.get(voiceRecord.getFilePath());
                Files.deleteIfExists(filePath);
                log.info("녹음 파일 삭제 완료: {}", filePath.toString());
            } catch (IOException e) {
                log.error("녹음 파일 삭제 실패: {}", voiceRecord.getFilePath(), e);
            }
            
            // DB에서 삭제
            voiceRecordMapper.deleteById(voiceId);
            log.info("녹음 파일 DB 삭제 완료: voiceId={}", voiceId);
        }
    }

    /**
     * 녹음 파일 다운로드용 파일 객체 반환
     */
    public File getVoiceRecordFile(Long voiceId) {
        VoiceRecord voiceRecord = voiceRecordMapper.findById(voiceId);
        if (voiceRecord == null) {
            throw new RuntimeException("녹음 파일을 찾을 수 없습니다.");
        }
        
        File file = new File(voiceRecord.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("녹음 파일이 존재하지 않습니다.");
        }
        
        return file;
    }
} 