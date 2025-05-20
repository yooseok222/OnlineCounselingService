package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.repository.PdfMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PdfService {
    
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
    // 실제 DB에 저장된 PDF의 메모리 캐시 (선택적으로 사용)
    private static final Map<String, byte[]> pdfMemoryCache = new HashMap<>();
    
    @Autowired
    private PdfMapper pdfMapper;
    
    // PDF 조회
    public PdfDTO getPdfById(Long pdfId) {
        return pdfMapper.selectPdfById(pdfId);
    }
    
    // 계약 ID로 PDF 조회
    public List<PdfDTO> getPdfsByContractId(Long contractId) {
        return pdfMapper.selectPdfsByContractId(contractId);
    }
    
    // PDF 파일 리소스 조회 (파일 시스템에서 읽거나 메모리 캐시에서 읽음)
    public ResponseEntity<Resource> getPdfResource(String fileName) {
        // 메모리 캐시에서 먼저 확인
        if (pdfMemoryCache.containsKey(fileName)) {
            byte[] pdfData = pdfMemoryCache.get(fileName);
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);
        }
        
        // 메모리에 없으면 파일 시스템에서 확인 (기존 DB 저장 파일을 위한 후속 처리)
        File file = new File(UPLOAD_DIR, fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 파일을 메모리로 로드해서 캐싱
        try {
            byte[] pdfData = Files.readAllBytes(file.toPath());
            pdfMemoryCache.put(fileName, pdfData);
            
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);
        } catch (IOException e) {
            // 파일 로드 실패 시 직접 FileSystemResource 사용
            FileSystemResource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        }
    }
    
    // PDF 업로드 및 저장 (메모리 사용)
    public PdfDTO uploadPdf(MultipartFile file, Long contractId) throws IOException {
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // 파일 내용 읽기
        byte[] fileBytes = file.getBytes();
        
        // 파일을 메모리에 저장
        pdfMemoryCache.put(uniqueFilename, fileBytes);
        
        // 파일 해시값 계산
        String fileHash = generateFileHash(fileBytes);
        
        // PDF 정보 DB에 저장
        PdfDTO pdfDTO = new PdfDTO();
        pdfDTO.setFilePath(uniqueFilename);
        pdfDTO.setFileHash(fileHash);
        pdfDTO.setCreatedAt(new Date());
        pdfDTO.setContractId(contractId);
        
        pdfMapper.insertPdf(pdfDTO);
        
        return pdfDTO;
    }
    
    // PDF 정보 업데이트
    public int updatePdf(PdfDTO pdf) {
        return pdfMapper.updatePdf(pdf);
    }
    
    // PDF 삭제
    public int deletePdf(Long pdfId) {
        PdfDTO pdf = getPdfById(pdfId);
        if (pdf != null) {
            // 메모리 캐시에서 삭제
            pdfMemoryCache.remove(pdf.getFilePath());
            
            // DB에서 삭제
            return pdfMapper.deletePdf(pdfId);
        }
        return 0;
    }
    
    // 파일 해시 생성
    private String generateFileHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    // 파일 해시 생성 (Path 버전 - 기존 코드 호환성 유지)
    private String generateFileHash(Path filePath) {
        try {
            byte[] data = Files.readAllBytes(filePath);
            return generateFileHash(data);
        } catch (IOException e) {
            return null;
        }
    }
} 