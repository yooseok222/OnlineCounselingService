package kr.or.kosa.visang.domain.agent.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateAgentDto {
    private Long agentId;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private String state; // ACTIVE, ON_LEAVE, RETIRED
    private String profileImageUrl;     // 이미지 저장 경로
    private MultipartFile profileImage; // 프로필 이미지 파일 업로드
}