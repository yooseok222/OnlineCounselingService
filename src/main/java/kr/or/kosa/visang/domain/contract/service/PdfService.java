package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.contract.repository.PdfMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.UUID;

@Service
public class PdfService {
    
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
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
    
    // PDF 업로드 및 저장
    public PdfDTO uploadPdf(MultipartFile file, Long contractId) throws IOException {
        // 파일을 서버 디렉토리에 저장
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // 파일 저장 경로
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFilename);
        file.transferTo(filePath.toFile());
        
        // 파일 해시값 계산
        String fileHash = generateFileHash(filePath);
        
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
            // 파일 시스템에서 삭제
            File file = new File(UPLOAD_DIR, pdf.getFilePath());
            if (file.exists()) {
                file.delete();
            }
            
            // DB에서 삭제
            return pdfMapper.deletePdf(pdfId);
        }
        return 0;
    }
    
    // 파일 해시 생성
    private String generateFileHash(Path filePath) {
        try {
            byte[] data = Files.readAllBytes(filePath);
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
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
} 