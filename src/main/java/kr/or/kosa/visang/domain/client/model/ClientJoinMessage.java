package kr.or.kosa.visang.domain.client.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientJoinMessage {
    private String userType;  // "client" 또는 "agent"
    private String userName;
    private String entryType; // "stamp" 또는 "signature"
    private String timestamp;
    
    // 기본 생성자
    public ClientJoinMessage() {
    }
    
    // 모든 필드 초기화 생성자
    public ClientJoinMessage(String userType, String userName, String entryType, String timestamp) {
        this.userType = userType;
        this.userName = userName;
        this.entryType = entryType;
        this.timestamp = timestamp;
    }
} 