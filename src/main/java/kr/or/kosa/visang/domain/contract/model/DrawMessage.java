package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawMessage {
    private String type;  // ✅ 'start' 또는 'move'
    private String mode;  // 'pen' or 'highlight'
    private int x;
    private int y;
    private int pageNumber; // ✅ 페이지 동기화를 위한 필드도 필요
}
