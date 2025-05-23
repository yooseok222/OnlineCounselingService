package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

@Data
public class ContractCompleteCountsByMonthDTO {
    private int firstMonthCount;
    private int secondMonthCount;
    private int thirdMonthCount;
    private int fourthMonthCount;
    private int fifthMonthCount;
}
