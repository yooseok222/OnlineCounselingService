package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.repository.PdfMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;
    
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
        System.out.println("PdfService.getPdfResource 호출: " + fileName);
        
        // 쿼리 파라미터 제거(타임스탬프 제거)
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
            System.out.println("쿼리 파라미터 제거 후 파일명: " + fileName);
        }
        
        // 특수 프리픽스 제거
        if (fileName.startsWith("pdf_")) {
            fileName = fileName.substring(4);
            System.out.println("프리픽스 제거 후 파일명: " + fileName);
        }
        
        // 메모리 캐시에서 먼저 확인
        if (pdfMemoryCache.containsKey(fileName)) {
            System.out.println("메모리 캐시에서 PDF 파일 찾음: " + fileName);
            byte[] pdfData = pdfMemoryCache.get(fileName);
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            
            // 캐시 방지를 위한 랜덤 값
            String etag = "\"" + UUID.randomUUID().toString() + "\"";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);
        }
        
        System.out.println("메모리 캐시에서 PDF 파일을 찾을 수 없음: " + fileName);
        
        // 메모리에 없으면 파일 시스템에서 확인 (기존 DB 저장 파일을 위한 후속 처리)
        File file = new File(uploadDirPdf, fileName);
        if (!file.exists()) {
            System.err.println("파일 시스템에서도 PDF 파일을 찾을 수 없음: " + fileName);
            return ResponseEntity.notFound().build();
        }

        System.out.println("파일 시스템에서 PDF 파일 찾음: " + fileName);
        
        // 파일을 메모리로 로드해서 캐싱
        try {
            byte[] pdfData = Files.readAllBytes(file.toPath());
            pdfMemoryCache.put(fileName, pdfData);
            System.out.println("파일 시스템의 PDF를 메모리 캐시에 저장: " + fileName);
            
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            
            // 캐시 방지를 위한 랜덤 값
            String etag = "\"" + UUID.randomUUID().toString() + "\"";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(resource);
        } catch (IOException e) {
            System.err.println("파일 로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            // 파일 로드 실패 시 직접 FileSystemResource 사용
            FileSystemResource resource = new FileSystemResource(file);
            
            // 캐시 방지를 위한 랜덤 값
            String etag = "\"" + UUID.randomUUID().toString() + "\"";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        }
    }
    
    // PDF 업로드 및 저장 (메모리 + 파일 시스템 사용)
    public PdfDTO uploadPdf(MultipartFile file, Long contractId) throws IOException {
        System.out.println("PdfService.uploadPdf: 파일명=" + file.getOriginalFilename() + ", 계약ID=" + contractId);
        
        // 임시 계약인 경우 (contractId < 0)
        boolean isTemporary = contractId < 0;
        
        // 기존 PDF 파일 삭제 (contractId에 해당하는 모든 PDF) - 임시 계약이 아닌 경우만
        if (!isTemporary) {
            List<PdfDTO> existingPdfs = getPdfsByContractId(contractId);
            if (existingPdfs != null && !existingPdfs.isEmpty()) {
                for (PdfDTO existingPdf : existingPdfs) {
                    // 기존 파일을 메모리 캐시에서 삭제
                    pdfMemoryCache.remove(existingPdf.getFilePath());
                    
                    // 파일 시스템에서도 삭제
                    try {
                        File existingFile = new File(uploadDirPdf, existingPdf.getFilePath());
                        if (existingFile.exists()) {
                            existingFile.delete();
                            System.out.println("기존 PDF 파일 삭제: " + existingFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        System.err.println("기존 파일 삭제 중 오류: " + e.getMessage());
                    }
                    
                    // DB에서 삭제
                    pdfMapper.deletePdf(existingPdf.getPdfId());
                }
            }
        }
        
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // 파일 내용 읽기
        byte[] fileBytes = file.getBytes();
        
        // 파일을 메모리에 저장
        System.out.println("PDF 메모리 캐시에 저장: " + uniqueFilename + " (" + fileBytes.length + " bytes)");
        pdfMemoryCache.put(uniqueFilename, fileBytes);
        
        // 파일 시스템에도 저장
        try {
            // 디렉토리 존재 확인 및 생성
            Path uploadPath = Paths.get(uploadDirPdf);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("업로드 디렉토리 생성: " + uploadPath.toAbsolutePath());
            }
            
            // 파일 저장
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, fileBytes);
            System.out.println("PDF 파일 시스템에 저장: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("파일 시스템에 저장 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류가 발생해도 메모리 캐시에는 저장되어 있으므로 계속 진행
        }
        
        // 파일 해시값 계산
        String fileHash = generateFileHash(fileBytes);
        
        // PDF 정보 객체 생성
        PdfDTO pdfDTO = new PdfDTO();
        pdfDTO.setFilePath(uniqueFilename);
        pdfDTO.setFileHash(fileHash);
        pdfDTO.setCreatedAt(new Date());
        pdfDTO.setContractId(contractId);
        
        // 임시 계약이 아닌 경우만 DB에 저장
        if (!isTemporary) {
            pdfMapper.insertPdf(pdfDTO);
            System.out.println("PDF 정보 DB에 저장: " + pdfDTO.getFilePath());
        } else {
            // 임시 파일에 대한 가상 ID 할당
            pdfDTO.setPdfId(-1L);
            System.out.println("임시 PDF 파일 (DB 저장 안함): " + pdfDTO.getFilePath());
        }
        
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