package kr.or.kosa.visang.domain.contract.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserJoinMessage {
    private String userType;  // "client" 또는 "agent"
    private String userName;
    private String entryType; // "stamp" 또는 "signature"
    private String timestamp;
    private String sessionId;
    
    // 기본 생성자
    public UserJoinMessage() {
    }
    
    // 모든 필드 초기화 생성자
    public UserJoinMessage(String userType, String userName, String entryType, String timestamp) {
        this.userType = userType;
        this.userName = userName;
        this.entryType = entryType;
        this.timestamp = timestamp;
    }
} 