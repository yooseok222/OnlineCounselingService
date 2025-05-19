package kr.or.kosa.visang.domain.chat.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Chat {
	private Long chatId;
	private Long contractId;
	private Long senderId;
	private String senderType;
	private String message;
	private String messageType;
	private LocalDateTime sendTime;
}
