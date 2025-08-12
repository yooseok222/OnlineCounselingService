package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawMessage {
    private String type;  // 'pen' 또는 'highlight' 또는 'start'/'move'
    private String mode;  // 'pen' or 'highlight'
    private int x;
    private int y;
    private int pageNumber; // 페이지 동기화를 위한 필드
    
    // 클라이언트에서 보내는 추가 필드
    private int lastX;
    private int lastY;
    private int currentX;
    private int currentY;
    private int page;
    private String sessionId;
    private String sender;
}
