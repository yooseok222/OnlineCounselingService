package kr.or.kosa.visang.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agent {
    private Long agentId;
    private String companyId;
    private String name;
    private String email;
    private String state; // 상태 (휴직, 퇴직, 복귀)
    private String phoneNumber;
    private String address;

    private String createdAt;
    private String profileImageUrl; // 이미지 저장 경로

//    // 추가 필드
//    private int totalCount; // 총 건수
//    private int pageCount;  // 페이지 수
//    private int pageNum;   // 현재 페이지 번호
}
