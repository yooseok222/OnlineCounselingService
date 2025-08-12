package kr.or.kosa.visang.common.file;
import kr.or.kosa.visang.domain.pdf.enums.PDFTYPE;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    @Value("${file.upload-dir.signed-pdf}")
    private String uploadDirSignedPdf;

    public String saveProfileImage(MultipartFile file, Long agentId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String newFileName = "agent_" + agentId + extension;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // URL로 접근할 수 있는 상대경로 리턴
        return "/images/profile/" + newFileName;
    }

    public Resource loadResource(String dbPath, PDFTYPE pdftype) {
        // PDFTYPE에 따라 저장 디렉토리 선택
        String targetDir = switch (pdftype) {
            case TEMPLATE_PDF -> uploadDirPdf; // 서명되지 않은 계약서 디렉토리
            case SIGNED_PDF -> uploadDirSignedPdf; // 서명된 계약서 디렉토리
        };

        //1 . DB에서 가져온 상대 경로 예:  "/files/pdf/template_5.pdf" 에서 파일명만 추출
        String fileName = Paths.get(dbPath).getFileName().toString();
            
        //2. 실제 저장소 경로에 결합
        Path filePath = Paths.get(targetDir,fileName); // dbPath 예: "C:/upload/pdf/template_5.pdf"
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로 문제: " + dbPath, e);
        }
    }

    public String savePDF(MultipartFile file, Long contractTemplateId, PDFTYPE pdftype) throws IOException {
        // PDFTYPE에 따라 저장 디렉토리 선택
        if (file == null || file.isEmpty()) {
            return null;
        }

        String targetDir = switch (pdftype) {
            case TEMPLATE_PDF -> uploadDirPdf; // 서명되지 않은 계약서 디렉토리
            case SIGNED_PDF -> uploadDirSignedPdf; // 서명된 계약서 디렉토리
        };
        if (targetDir == null || targetDir.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 PDF 저장 디렉토리: " + pdftype);
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
//        String newFileName = "template_" + contractTemplateId + extension;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(fmt);
        String fn = String.format("template_%d_%s_%s%s",
                contractTemplateId, timestamp, UUID.randomUUID(), extension);

        Path uploadPath = Paths.get(targetDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fn);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // URL로 접근할 수 있는 상대경로 리턴
        return "/files/pdf/" + fn;
    }
}
