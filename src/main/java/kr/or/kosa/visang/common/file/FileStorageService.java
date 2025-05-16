package kr.or.kosa.visang.common.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

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
}
