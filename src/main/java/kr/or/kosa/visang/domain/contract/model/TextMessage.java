package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextMessage {
    private int x;
    private int y;
    private int pageNumber;
    private String text;
    
    // 클라이언트에서 보내는 추가 필드
    private String type;
    private int page;
    private String sessionId;
    private String sender;
}

