package kr.or.kosa.visang.domain.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    /**
     * 메시지 ID (자동 생성)
     */
    private Long messageId;

    /**
     * 세션 ID
     */
    private String sessionId;

    /**
     * 계약 ID
     */
    private Long contractId;

    /**
     * 메시지 타입 (CHAT, JOIN, LEAVE, SYSTEM)
     */
    private MessageType type;

    /**
     * 발신자 (agent, client)
     */
    private String sender;

    /**
     * 발신자 이름
     */
    private String senderName;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 전송 시간 (서버에서 자동 설정)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    /**
     * 읽음 여부
     */
    private boolean isRead;

    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        CHAT,    // 일반 채팅 메시지
        JOIN,    // 방 입장
        LEAVE,   // 방 나가기
        SYSTEM   // 시스템 메시지
    }

    /**
     * 채팅 메시지 생성 편의 메서드
     */
    public static ChatMessage createChatMessage(String sessionId, Long contractId, String sender, String senderName, String content) {
        return ChatMessage.builder()
                .sessionId(sessionId)
                .contractId(contractId)
                .type(MessageType.CHAT)
                .sender(sender)
                .senderName(senderName)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    /**
     * 시스템 메시지 생성 편의 메서드
     */
    public static ChatMessage createSystemMessage(String sessionId, Long contractId, String content) {
        return ChatMessage.builder()
                .sessionId(sessionId)
                .contractId(contractId)
                .type(MessageType.SYSTEM)
                .sender("system")
                .senderName("시스템")
                .content(content)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    /**
     * 입장 메시지 생성 편의 메서드
     */
    public static ChatMessage createJoinMessage(String sessionId, Long contractId, String sender, String senderName) {
        return ChatMessage.builder()
                .sessionId(sessionId)
                .contractId(contractId)
                .type(MessageType.JOIN)
                .sender(sender)
                .senderName(senderName)
                .content(senderName + "님이 입장했습니다.")
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();
    }
}
