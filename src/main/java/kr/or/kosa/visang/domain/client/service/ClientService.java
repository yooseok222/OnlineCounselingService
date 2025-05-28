package kr.or.kosa.visang.domain.client.service;

import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {
    
    private final ClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @PostConstruct
    public void init() {
        // 업로드 디렉토리 생성
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("프로필 이미지 업로드 디렉토리 생성: {}", uploadPath);
            }
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 디렉토리 생성 실패: {}", uploadDir, e);
        }
    }
    
    /**
     * 고객 정보 조회
     */
    public Client getClientById(Long clientId) {
        return clientMapper.findById(clientId);
    }
    
    /**
     * 이메일로 고객 정보 조회
     */
    public Client getClientByEmail(String email) {
        return clientMapper.findByEmail(email);
    }
    
    /**
     * 고객 정보 수정
     */
    @Transactional
    public void updateClient(Client client) {
        Client existingClient = clientMapper.findById(client.getClientId());
        if (existingClient == null) {
            throw new IllegalArgumentException("고객 정보를 찾을 수 없습니다.");
        }
        
        // 기본 정보 업데이트
        existingClient.setName(client.getName());
        existingClient.setPhoneNumber(client.getPhoneNumber());
        existingClient.setAddress(client.getAddress());
        
        // Mapper를 통해 업데이트
        clientMapper.update(existingClient);
    }
    
    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long clientId, String currentPassword, String newPassword) {
        Client client = clientMapper.findById(clientId);
        if (client == null) {
            throw new IllegalArgumentException("고객 정보를 찾을 수 없습니다.");
        }
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, client.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 유효성 검증
        client.setPassword(newPassword);
        client.validatePassword();
        
        // 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(newPassword);
        clientMapper.updatePassword(clientId, encodedPassword);
        
        log.info("고객 비밀번호 변경 완료 - clientId: {}", clientId);
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public String uploadProfileImage(Long clientId, MultipartFile file) throws IOException {
        // 파일 유효성 검사
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        // 이미지 파일인지 확인
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        
        // 파일 크기 제한 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }
        
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = "profile_" + clientId + "_" + UUID.randomUUID().toString() + extension;
        
        // 파일 저장
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // DB에 파일 경로 저장
        String profileImageUrl = "/images/profile/" + uniqueFilename;
        clientMapper.updateProfileImageUrl(clientId, profileImageUrl);
        
        log.info("프로필 이미지 업로드 완료 - clientId: {}, filename: {}", clientId, uniqueFilename);
        
        return profileImageUrl;
    }
    
    /**
     * 프로필 이미지 삭제
     */
    @Transactional
    public void deleteProfileImage(Long clientId) {
        Client client = clientMapper.findById(clientId);
        if (client == null) {
            throw new IllegalArgumentException("고객 정보를 찾을 수 없습니다.");
        }
        
        String profileImageUrl = client.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty() && !profileImageUrl.contains("default_profile")) {
            // 파일 시스템에서 이미지 삭제
            try {
                String filename = profileImageUrl.substring(profileImageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir).resolve(filename);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("프로필 이미지 삭제 실패 - clientId: {}, path: {}", clientId, profileImageUrl, e);
            }
            
            // DB에서 URL을 default 이미지로 설정
            clientMapper.updateProfileImageUrl(clientId, "/images/profile/default_profile.png");
            
            log.info("프로필 이미지 삭제 완료 - clientId: {}", clientId);
        }
    }
} 