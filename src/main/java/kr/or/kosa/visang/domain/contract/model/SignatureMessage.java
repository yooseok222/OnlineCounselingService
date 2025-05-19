package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignatureMessage {
    private int x;
    private int y;
    private int pageNumber;
    private String image; // base64 string
    
    // 기본 생성자
    public SignatureMessage() {
    }
    
    // 모든 필드 초기화 생성자
    public SignatureMessage(int x, int y, int pageNumber, String image) {
        this.x = x;
        this.y = y;
        this.pageNumber = pageNumber;
        this.image = image;
    }
} 