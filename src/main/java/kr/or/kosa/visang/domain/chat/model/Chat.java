package kr.or.kosa.visang.domain.chat.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Chat {
	private Long chatId;
	private Long contractId;
	private String chatContent;
	private String chatType;
	private LocalDateTime sendTime;
}
