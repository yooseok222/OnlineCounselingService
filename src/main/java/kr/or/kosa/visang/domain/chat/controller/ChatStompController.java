package kr.or.kosa.visang.domain.chat.controller;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import kr.or.kosa.visang.domain.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatStompController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    public ChatStompController(SimpMessagingTemplate template, ChatService chatService) {

        this.template = template;
        this.chatService = chatService;
    }


    // STOMP 메시지 전송
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {
        System.out.println("▶▶▶ sendMessage() 호출됨: roomId="
                + message.getRoomId() + ", content="
                + message.getContent() + ", principal="
                + principal.getName());

        // 로그인한 사용자로 설정
        message.setSender(principal.getName());

        // DB 저장
        chatService.saveMessageToRedis(message);
        System.out.println("▶ sendMessage() 진입: " + message);

        // 브로드 캐스트
        template.convertAndSend("/topic/chat/" + message.getRoomId(), message);
    }

    // STOMP : 사용자 입장 알림
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage message, Principal principal) {
        System.out.println("▶▶▶ addUser() 호출됨: roomId="
                + message.getRoomId() + ", principal="
                + principal.getName());

        // 로그인된  사용자 이름으로 덮어쓰기
        message.setSender(principal.getName());

        // 입장 알림 저장(optional)
        chatService.saveMessageToRedis(message);

        //토픽 발행
        template.convertAndSend("/topic/chat/" + message.getRoomId(), message);

    }

    public void sendMessage(ChatMessage message) {
        // 1) Redis에 쌓기
        chatService.saveMessageToRedis(message);

        // 2) 브로드캐스트
        template.convertAndSend("/topic/chat/" + message.getRoomId(), message);
    }

    @MessageMapping("/chat.endCall")
    public void endCall(@Payload ChatMessage message, Principal principal) {
        Long roomId = message.getRoomId();
        String user = principal.getName();

        // 1) Redis - 파일 - chat.export_filepath 업데이트
        chatService.endAndExport(roomId, user);

        ChatMessage endMsg = new ChatMessage();
        endMsg.setRoomId(roomId);
        endMsg.setSender(user);
        endMsg.setContent("통화를 종료하고 이력을 파일로 저장했습니다.");
        endMsg.setType("END");
        endMsg.setSendTime(LocalDateTime.now());
        template.convertAndSend("/topic/chat/" + roomId, endMsg);
    }
}



