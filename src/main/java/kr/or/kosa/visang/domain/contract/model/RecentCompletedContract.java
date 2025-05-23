package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class RecentCompletedContract {
    private String contractTime; // 계약 시간

    private String agentName; // 상담사 이름
    private String clientName; // 고객 이름
}
