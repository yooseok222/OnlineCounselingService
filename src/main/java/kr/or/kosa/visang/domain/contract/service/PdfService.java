package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.common.config.hash.HashUtil;
import kr.or.kosa.visang.domain.contract.model.ContractSingedDTO;
import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.repository.PdfMapper;
import kr.or.kosa.visang.domain.pdf.service.PdfSignerService;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PdfService {
    
    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${file.upload-dir.signed-pdf}")
    private String signedPdfPath;
    
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
    
    // PDF 파일 리소스 조회 (메모리 캐시에서만 읽음)
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
        
        // 메모리 캐시에서 확인
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
        
        // 2. 파일 시스템에서 확인 (최종 PDF용 - 메모리에서 찾지 못한 경우)
        try {
            Path filePath = Paths.get(uploadDirPdf, fileName);
            if (Files.exists(filePath)) {
                System.out.println("파일 시스템에서 PDF 파일 찾음: " + filePath);
                FileSystemResource resource = new FileSystemResource(filePath);

                // 캐시 방지를 위한 랜덤 값
                String etag = "\"" + UUID.randomUUID().toString() + "\"";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileName)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .header(HttpHeaders.ETAG, etag)
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(Files.size(filePath))
                        .body(resource);
            }
        } catch (Exception e) {
            System.err.println("파일 시스템에서 PDF 파일 읽기 오류: " + e.getMessage());
        }

        System.out.println("메모리 캐시와 파일 시스템에서 PDF 파일을 찾을 수 없음: " + fileName);
        return ResponseEntity.notFound().build();
    }

    public PdfDTO uploadPdf(MultipartFile file, Long contractId) throws IOException {
        System.out.println("PdfService.uploadPdf: 파일명=" + file.getOriginalFilename() + ", 계약ID=" + contractId);

        // 임시 계약인 경우 (contractId < 0)
        boolean isTemporary = contractId < 0;

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        // 파일 내용 읽기
        byte[] fileBytes = file.getBytes();

        // 파일명으로 원본 PDF와 최종 PDF 구분
        boolean isFinalPdf = originalFilename.contains("상담문서_");

        // 모든 PDF는 메모리에 저장
        System.out.println("PDF 메모리 캐시에 저장: " + uniqueFilename + " (" + fileBytes.length + " bytes)");
        pdfMemoryCache.put(uniqueFilename, fileBytes);


        // PDF 정보 객체 생성
        PdfDTO pdfDTO = new PdfDTO();
        pdfDTO.setFilePath(uniqueFilename);

        pdfDTO.setCreatedAt(new Date());
        pdfDTO.setContractId(contractId);


        // 임시 파일에 대한 가상 ID 할당
        pdfDTO.setPdfId(-1L);
        System.out.println("임시 PDF 파일 (DB 저장 안함): " + pdfDTO.getFilePath());


        return pdfDTO;
    };
    
    // PDF 업로드 및 저장 (메모리 + 파일 시스템 사용)
    public PdfDTO uploadFinalPdf(MultipartFile file, Long contractId) throws IOException, NoSuchAlgorithmException {
        System.out.println("PdfService.uploadPdf: 파일명=" + file.getOriginalFilename() + ", 계약ID=" + contractId);

        // 업로드된 PDF 파일(MultipartFile)을 InputStream으로 변환
        InputStream inputStreamPDF = file.getInputStream();

        String outputFileName = createOutputFileName(file.getOriginalFilename(), contractId);

        try {
            ContractSingedDTO contractSignedDTO = pdfMapper.selectSignedContractInfoByContractId(contractId);

            PdfSignerService.signPdf(
                    inputStreamPDF,     // 원본 PDF
                    outputFileName,
                    keyStorePath,           // 생성한 키스토어
                    keyStorePassword,                    // 비밀번호
                    "contract-signing-key",            // alias
                    signedPdfPath,
                    contractSignedDTO
            );
            System.out.println("✅ 서명 완료: contract-signed.pdf");
        } catch (Exception e) {
            System.err.println("서명 중 오류 발생: " + e.getMessage());
        }

        Path signedFilePath = Paths.get(signedPdfPath).resolve(outputFileName);
        String hash = HashUtil.sha256(signedFilePath.toString());

        //DB 저장 주소
        String dbFilePath = "files/signed_pdf/" + outputFileName;
        
        // PDF 정보 객체 생성
        PdfDTO pdfDTO = new PdfDTO();
        pdfDTO.setFilePath(dbFilePath);
        pdfDTO.setFileHash(hash);
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

    private String createOutputFileName(String originalFileName, Long contractId) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("Original file name cannot be null or empty");
        }
        if (contractId == null || contractId <= 0) {
            throw new IllegalArgumentException("Contract ID must be a positive number");
        }
        System.out.println("Creating output file name for contract ID: " + contractId + ", original file name: " + originalFileName);

        // 파일 이름에서 확장자 추출
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        // UUID를 사용하여 고유한 파일 이름 생성

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(fmt);

        return String.format("signed_%d_%s_%s%s", contractId, timestamp, UUID.randomUUID(), extension);
    }
} 