package kr.or.kosa.visang.domain.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private Long chatId;
    private Long roomId;
    private String sender;  //  agent , client 구분
    private String content; // 채팅 내용
    private String type; // TEXT
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sendTime;
    private String exportFilePath;
}
