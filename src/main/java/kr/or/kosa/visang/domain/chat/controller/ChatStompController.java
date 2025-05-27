package kr.or.kosa.visang.domain.chat.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import kr.or.kosa.visang.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * 레거시 채팅 시스템을 위한 STOMP 컨트롤러
 * 기존 시스템과의 하위 호환성을 위해 유지
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    /**
     * 레거시 채팅 메시지 전송 (기존 시스템과의 호환성을 위해 유지)
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessageLegacy(@Payload ChatMessage message, Authentication authentication) {
        try {
            log.info("레거시 채팅 메시지 수신: contractId={}", message.getContractId());
            
            // 로그인한 사용자로 설정
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
                message.setSender(user.getName());
            }
            
            // 시간 설정
            message.setTimestamp(LocalDateTime.now());
            
            // DB 저장
            chatService.saveMessageToRedis(message);

            // 브로드 캐스트 (레거시 토픽 사용)
            template.convertAndSend("/topic/chat/" + message.getContractId(), message);
            
        } catch (Exception e) {
            log.error("레거시 채팅 메시지 처리 중 오류 발생", e);
        }
    }

    /**
     * 레거시 사용자 입장 알림 (기존 시스템과의 호환성을 위해 유지)
     */
    @MessageMapping("/chat.addUser")
    public void addUserLegacy(@Payload ChatMessage message, Authentication authentication) {
        try {
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
                message.setSender(user.getName());
            }
            
            message.setType(ChatMessage.MessageType.JOIN);
            message.setTimestamp(LocalDateTime.now());

            // 입장 알림 저장(optional)
            chatService.saveMessageToRedis(message);

            //토픽 발행 (레거시 토픽 사용)
            template.convertAndSend("/topic/chat/" + message.getContractId(), message);
            
        } catch (Exception e) {
            log.error("레거시 사용자 입장 처리 중 오류 발생", e);
        }
    }

    /**
     * 레거시 상담 종료 (기존 시스템과의 호환성을 위해 유지)
     */
    @MessageMapping("/chat.endCall")
    public void endCallLegacy(@Payload ChatMessage message, Authentication authentication) {
        try {
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
                
                Long roomId = message.getContractId();
                String name = user.getName();

                // 1) Redis - 파일 - chat.export_filepath 업데이트
                chatService.endAndExport(roomId, name);

                ChatMessage endMsg = new ChatMessage();
                endMsg.setContractId(roomId);
                endMsg.setSender(name);
                endMsg.setContent("통화를 종료하고 이력을 파일로 저장했습니다.");
                endMsg.setType(ChatMessage.MessageType.SYSTEM);
                endMsg.setTimestamp(LocalDateTime.now());
                template.convertAndSend("/topic/chat/" + roomId, endMsg);
            }
        } catch (Exception e) {
            log.error("레거시 상담 종료 처리 중 오류 발생", e);
        }
    }
}



