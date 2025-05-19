package kr.or.kosa.visang.domain.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contract {
    private Long contractId; // 계약 ID
    private String status; // 계약 상태 (예: 진행 중, 완료, 취소 등)
    private String createdAt; // 계약 생성일
    private String contractTime; // 계약 시간
    private String companyId; // 회사 ID

    private String agentId; // 상담사 ID
    private String clientId; // 고객 ID

    private String templateId; // 계약서 템플릿 ID
    private String  memo; // 계약 메모

//    // 추가 필드
//    private int totalCount; // 총 건수
//    private int pageCount;  // 페이지 수
//    private int pageNum;   // 현재 페이지 번호
}
