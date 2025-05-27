package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class PdfUploadController {

    // 파일 시스템 대신 메모리에 PDF 저장
    private static final Map<String, byte[]> inMemoryPdfStorage = new ConcurrentHashMap<>();
    
    @Autowired
    private PdfService pdfService;

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file, 
                                             @RequestParam(value = "contractId", required = false) Long contractId) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("PDF 업로드 요청: 파일명=" + fileName + ", 크기=" + file.getSize() + " bytes");

        try {
            // PDF DTO 생성
            PdfDTO pdfDTO;
            
            // 계약 ID가 있으면 DB에 저장, 없으면 임시 ID 생성
            if (contractId == null) {
                // 임시 계약 ID 사용 (테스트용)
                contractId = -1L;
                System.out.println("임시 계약 ID 사용: " + contractId);
            }
            
            // 항상 동일한 서비스 메서드 사용
            pdfDTO = pdfService.uploadPdf(file, contractId);
            
            // 응답 URL에 타임스탬프 추가
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileUrl = "/files/pdf/" + pdfDTO.getFilePath() + "?t=" + timestamp + "&nocache=" + Math.random();
            
            System.out.println("PDF 업로드 성공: URL=" + fileUrl);
            
            return ResponseEntity.ok()
                    // 캐시 방지 헤더 추가 (더 엄격한 설정)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Accel-Expires", "0")  // nginx 캐시 방지
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("ETag", "\"" + System.nanoTime() + "\"")  // 고유한 ETag 생성
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(fileUrl);
            
        } catch (IOException e) {
            System.err.println("PDF 업로드 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/files/pdf/{fileId}")
    @ResponseBody
    public ResponseEntity<Resource> servePdf(@PathVariable String fileId) {
        System.out.println("PDF 파일 요청: " + fileId);
        
        // 쿼리 파라미터 제거(타임스탬프 제거)
        if (fileId.contains("?")) {
            fileId = fileId.substring(0, fileId.indexOf("?"));
        }
        
        // 모든 PDF 요청을 서비스로 처리
        ResponseEntity<Resource> response = pdfService.getPdfResource(fileId);
        
        // 응답 로깅
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("PDF 파일 응답 성공: " + fileId);
        } else {
            System.err.println("PDF 파일 찾을 수 없음: " + fileId);
        }
        
        return response;
    }
    
    @GetMapping("/api/pdfs/contract/{contractId}")
    @ResponseBody
    public ResponseEntity<?> getPdfsByContractId(@PathVariable Long contractId) {
        try {
            return ResponseEntity.ok(pdfService.getPdfsByContractId(contractId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("PDF 조회 실패: " + e.getMessage());
        }
    }

    // 이전 URL 경로도 지원 (하위 호환성)
    @GetMapping("/pdf/{fileId}")
    @ResponseBody
    public ResponseEntity<Resource> servePdfLegacy(@PathVariable String fileId) {
        System.out.println("이전 경로로 PDF 파일 요청 (리다이렉트): " + fileId);
        return servePdf(fileId);
    }
}
