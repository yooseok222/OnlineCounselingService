package kr.or.kosa.visang.domain.chat.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import kr.or.kosa.visang.domain.chat.model.Chat;

@Controller
public class ChatController {

	@MessageMapping("/chat.send/{roomId}") // 클라이언트 → 서버
	@SendTo("/topic/chat/{roomId}") // 서버 → 구독자
	public Chat send(@DestinationVariable String rommId, Chat message) {
		return message;
	}
}