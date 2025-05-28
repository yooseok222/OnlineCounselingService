package kr.or.kosa.visang.domain.chat.controller;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import kr.or.kosa.visang.domain.contract.service.SessionContractMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket 기반 채팅 컨트롤러
 * 세션 기반으로 채팅 메시지를 처리하고 전달
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionContractMappingService mappingService;
    private final kr.or.kosa.visang.domain.chat.service.ChatService chatService;

    /**
     * 채팅 메시지 전송 처리
     * @param sessionId WebSocket 세션 ID
     * @param message 채팅 메시지
     */
    @MessageMapping("/room/{sessionId}/chat.send")
    public void sendMessage(@DestinationVariable String sessionId, @Payload ChatMessage message) {
        try {
            log.info("채팅 메시지 수신: sessionId={}, sender={}, type={}", sessionId, message.getSender(), message.getType());
            
            // 1. 세션 유효성 검증
            Long contractId = mappingService.getContractIdBySession(sessionId);
            if (contractId == null) {
                log.warn("유효하지 않은 세션으로 채팅 메시지 전송 시도: sessionId={}", sessionId);
                return;
            }
            
            // 2. 메시지 데이터 보완
            message.setSessionId(sessionId);
            message.setContractId(contractId);
            
            // 타임스탬프가 없거나 null이면 현재 시간으로 설정
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }
            
            // 발신자 이름 설정 (필요시)
            if (message.getSenderName() == null || message.getSenderName().trim().isEmpty()) {
                message.setSenderName("agent".equals(message.getSender()) ? "상담원" : "고객");
            }
            
            // 메시지 타입이 null이면 CHAT으로 설정
            if (message.getType() == null) {
                message.setType(ChatMessage.MessageType.CHAT);
            }
            
            // 3. 메시지 타입별 처리
            switch (message.getType()) {
                case CHAT:
                    handleChatMessage(sessionId, message);
                    break;
                case JOIN:
                    handleJoinMessage(sessionId, message);
                    break;
                case LEAVE:
                    handleLeaveMessage(sessionId, message);
                    break;
                case SYSTEM:
                    handleSystemMessage(sessionId, message);
                    break;
                default:
                    log.warn("알 수 없는 메시지 타입: {}", message.getType());
            }
            
            // 4. 세션 활동 시간 업데이트
            mappingService.updateLastAccess(sessionId);
            
        } catch (Exception e) {
            log.error("채팅 메시지 처리 중 오류 발생: sessionId={}, message={}", sessionId, message, e);
        }
    }

    /**
     * 일반 채팅 메시지 처리
     */
    private void handleChatMessage(String sessionId, ChatMessage message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            log.warn("빈 채팅 메시지 무시: sessionId={}", sessionId);
            return;
        }
        
        log.info("채팅 메시지 전송: sessionId={}, sender={}, content={}", 
                sessionId, message.getSender(), message.getContent().substring(0, Math.min(message.getContent().length(), 50)));
        
        // Redis에 채팅 메시지 저장
        try {
            chatService.saveMessageToRedis(message);
            log.debug("채팅 메시지 Redis 저장 완료: sessionId={}, contractId={}", sessionId, message.getContractId());
        } catch (Exception e) {
            log.error("채팅 메시지 Redis 저장 실패: sessionId={}, contractId={}", sessionId, message.getContractId(), e);
        }
        
        // 세션의 모든 참여자에게 메시지 전송
        messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/chat", message);
    }

    /**
     * 방 입장 메시지 처리
     */
    private void handleJoinMessage(String sessionId, ChatMessage message) {
        log.info("방 입장 메시지: sessionId={}, sender={}", sessionId, message.getSender());
        
        // Redis에 입장 메시지 저장
        try {
            chatService.saveMessageToRedis(message);
            log.debug("입장 메시지 Redis 저장 완료: sessionId={}, contractId={}", sessionId, message.getContractId());
        } catch (Exception e) {
            log.error("입장 메시지 Redis 저장 실패: sessionId={}, contractId={}", sessionId, message.getContractId(), e);
        }
        
        // 입장 메시지를 다른 참여자들에게 전송
        messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/chat", message);
    }

    /**
     * 방 나가기 메시지 처리
     */
    private void handleLeaveMessage(String sessionId, ChatMessage message) {
        log.info("방 나가기 메시지: sessionId={}, sender={}", sessionId, message.getSender());
        
        // 나가기 메시지를 다른 참여자들에게 전송
        messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/chat", message);
    }

    /**
     * 시스템 메시지 처리
     */
    private void handleSystemMessage(String sessionId, ChatMessage message) {
        log.info("시스템 메시지: sessionId={}, content={}", sessionId, message.getContent());
        
        // 시스템 메시지를 모든 참여자에게 전송
        messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/chat", message);
    }

    /**
     * 채팅방 입장 알림 (별도 엔드포인트)
     * @param sessionId WebSocket 세션 ID
     * @param joinInfo 입장 정보
     */
    @MessageMapping("/room/{sessionId}/chat/join")
    public void joinRoom(@DestinationVariable String sessionId, @Payload ChatMessage joinInfo) {
        try {
            log.info("채팅방 입장: sessionId={}, sender={}", sessionId, joinInfo.getSender());
            
            // 세션 유효성 검증
            Long contractId = mappingService.getContractIdBySession(sessionId);
            if (contractId == null) {
                log.warn("유효하지 않은 세션으로 방 입장 시도: sessionId={}", sessionId);
                return;
            }
            
            // 입장 메시지 생성
            ChatMessage joinMessage = ChatMessage.createJoinMessage(
                sessionId, 
                contractId, 
                joinInfo.getSender(), 
                joinInfo.getSenderName() != null ? joinInfo.getSenderName() : 
                    ("agent".equals(joinInfo.getSender()) ? "상담원" : "고객")
            );
            
            // 타임스탬프 설정
            joinMessage.setTimestamp(LocalDateTime.now());
            
            // 입장 메시지 전송
            messagingTemplate.convertAndSend("/topic/room/" + sessionId + "/chat", joinMessage);
            
        } catch (Exception e) {
            log.error("채팅방 입장 처리 중 오류 발생: sessionId={}", sessionId, e);
        }
    }
} 