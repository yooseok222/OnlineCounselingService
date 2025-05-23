package kr.or.kosa.visang.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long chatId;
    private Long roomId;
    private String sender;  //  agent , client 구분
    private String content; // 채팅 내용
    private String type; // TEXT
    private LocalDateTime sendTime;
    private String exportFilePath;
}
