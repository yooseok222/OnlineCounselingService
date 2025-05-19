package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
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

@Controller
public class PdfUploadController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
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
                // 기존 로직 유지 (임시 업로드)
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) dir.mkdirs();

                File uploadedFile = new File(dir, fileName);
                file.transferTo(uploadedFile);

                return ResponseEntity.ok("/pdf/" + fileName);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/pdf/{fileName}")
    @ResponseBody
    public ResponseEntity<Resource> servePdf(@PathVariable String fileName) {
        File file = new File(UPLOAD_DIR, fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getName())
                .contentType(MediaType.APPLICATION_PDF)
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
