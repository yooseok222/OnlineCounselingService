package kr.or.kosa.visang.common.file;
import kr.or.kosa.visang.domain.contractTemplate.service.ContractTemplateService;
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

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

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

    public String savePDF(MultipartFile file, Long contractTemplateId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String newFileName = "template_" + contractTemplateId + extension;

        Path uploadPath = Paths.get(uploadDirPdf);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // URL로 접근할 수 있는 상대경로 리턴
        return "/files/pdf/" + newFileName;
    }
    public Resource loadTemplateAsResource(String dbPath) {
            //1 . DB에서 가져온 상대 경로 예:  "/files/pdf/template_5.pdf" 에서 파일명만 추출    
            String fileName = Paths.get(dbPath).getFileName().toString();
            
            //2. 실제 저장소 경로에 결합
            Path filePath = Paths.get(uploadDirPdf,fileName); // dbPath 예: "C:/upload/pdf/template_5.pdf"
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
}
