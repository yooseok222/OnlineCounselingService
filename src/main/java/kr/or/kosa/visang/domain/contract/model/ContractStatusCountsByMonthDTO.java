package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class ContractStatusCountsByMonthDTO {
    private int totalCount;
    private int pendingCount;
    private int completedCount;
    private int canceledCount;
    private int inProgressCount;
}
