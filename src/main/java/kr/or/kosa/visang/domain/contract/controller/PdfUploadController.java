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

        try {
            // 계약 ID가 있으면 DB에 저장
            if (contractId != null) {
                PdfDTO pdfDTO = pdfService.uploadPdf(file, contractId);
                return ResponseEntity.ok("/pdf/" + pdfDTO.getFilePath());
            } else {
                // 메모리에 저장 (임시 업로드)
                String fileId = "pdf_" + UUID.randomUUID().toString();
                byte[] fileBytes = file.getBytes();
                inMemoryPdfStorage.put(fileId, fileBytes);
                
                return ResponseEntity.ok("/pdf/" + fileId);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/pdf/{fileId}")
    @ResponseBody
    public ResponseEntity<Resource> servePdf(@PathVariable String fileId) {
        // DB에 저장된 파일인지 확인 (fileId가 UUID 형식이 아닌 경우)
        if (!fileId.startsWith("pdf_")) {
            return pdfService.getPdfResource(fileId);
        }
        
        // 메모리에서 PDF 데이터 조회
        byte[] pdfData = inMemoryPdfStorage.get(fileId);
        if (pdfData == null) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(pdfData);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + fileId)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfData.length)
                .body(resource);
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
    
    @DeleteMapping("/api/pdfs/{pdfId}")
    @ResponseBody
    public ResponseEntity<?> deletePdf(@PathVariable Long pdfId) {
        try {
            int result = pdfService.deletePdf(pdfId);
            if (result > 0) {
                return ResponseEntity.ok("PDF가 성공적으로 삭제되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("PDF 삭제 실패: 해당 ID의 PDF를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("PDF 삭제 실패: " + e.getMessage());
        }
    }
}
