package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StampMessage {
    private int x;
    private int y;
    private int pageNumber;
    private String image; // base64 string
}
