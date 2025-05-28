package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.StampDTO;
import kr.or.kosa.visang.domain.contract.repository.StampMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class StampService {

    @Value("${file.upload-dir.stamp}")
    private String UPLOAD_DIR;

    @Autowired
    private StampMapper stampMapper;

    // 도장 조회
    public StampDTO getStampById(Long stampId) {
        return stampMapper.selectStampById(stampId);
    }

    // 고객 ID로 도장 목록 조회
    public List<StampDTO> getStampsByClientId(Long clientId) {
        return stampMapper.selectStampsByClientId(clientId);
    }

    // 도장 이미지 업로드 및 저장 (기존 도장이 있으면 업로드 거부)
    public StampDTO uploadStamp(MultipartFile file, Long clientId) throws IOException {
        // 기존 도장이 있는지 확인
        List<StampDTO> existingStamps = getStampsByClientId(clientId);
        if (!existingStamps.isEmpty()) {
            throw new IllegalStateException("이미 등록된 도장이 있습니다. 새 도장을 업로드하려면 먼저 기존 도장을 삭제해주세요.");
        }
        
        // 업로드 경로를 절대 경로로 변환
        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        File uploadDir = uploadPath.toFile();
        
        // 업로드 폴더 생성
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("업로드 디렉토리를 생성할 수 없습니다: " + uploadPath);
            }
        }

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장 경로
        Path filePath = uploadPath.resolve(uniqueFilename);
        file.transferTo(filePath.toFile());

        // 도장 정보 DB에 저장
        StampDTO stampDTO = new StampDTO();
        stampDTO.setStampImageUrl("/stamp/image/" + uniqueFilename);
        stampDTO.setCreatedAt(new Date());
        stampDTO.setClientId(clientId);

        stampMapper.insertStamp(stampDTO);
        
        // 반환할 때 전체 경로 설정
        stampDTO.setStampImageUrl("/stamp/image/" + uniqueFilename);

        return stampDTO;
    }

    // 도장 정보 업데이트
    public int updateStamp(StampDTO stamp) {
        return stampMapper.updateStamp(stamp);
    }

    // 도장 삭제
    public int deleteStamp(Long stampId) {
        StampDTO stamp = getStampById(stampId);
        if (stamp != null) {
            // 파일 시스템에서 삭제
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(stamp.getStampImageUrl());
            File file = filePath.toFile();
            if (file.exists()) {
                file.delete();
            }

            // DB에서 삭제
            return stampMapper.deleteStamp(stampId);
        }
        return 0;
    }
} 