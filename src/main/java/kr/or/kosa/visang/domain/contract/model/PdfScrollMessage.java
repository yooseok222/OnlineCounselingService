package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PdfScrollMessage {
    private int pageNumber;
    private int scrollTop;
}
