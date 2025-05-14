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
    private String phoneNumber;
    private String address;

    private String createdAt;

//    // 추가 필드
//    private int totalCount; // 총 건수
//    private int pageCount;  // 페이지 수
//    private int pageNum;   // 현재 페이지 번호
}
