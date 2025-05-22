package kr.or.kosa.visang.domain.contract.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * WebRTC 시그널링 서버 컨트롤러
 * 브라우저 간 WebRTC 연결을 위한 시그널링 메시지를 중계합니다.
 */
@Controller
public class WebRTCController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCController.class);
    
    // roomId별 참가자를 추적하기 위한 맵
    private static final Map<String, Integer> roomParticipants = new HashMap<>();
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * WebRTC 시그널링 메시지 처리
     * 클라이언트에서 보낸 시그널링 메시지를 다른 클라이언트로 전달합니다.
     */
    @MessageMapping("/room/{roomId}/rtc")
    public void handleWebRTCMessage(@DestinationVariable String roomId, Map<String, Object> message) {
        String type = (String) message.get("type");
        
        if (roomId != null) {
            logger.debug("WebRTC {} 메시지 수신 (룸: {})", type, roomId);
            
            // join 메시지 처리 - 참가자 카운팅
            if ("join".equals(type)) {
                int count = roomParticipants.getOrDefault(roomId, 0) + 1;
                roomParticipants.put(roomId, count);
                logger.debug("룸 {} 참가자 수: {}", roomId, count);
                
                // join 응답에 현재 참가자 수 추가
                message.put("participants", count);
                message.put("timestamp", System.currentTimeMillis());
            }
            
            // leave 메시지 처리 - 참가자 카운트 감소
            if ("leave".equals(type) && roomParticipants.containsKey(roomId)) {
                int count = Math.max(0, roomParticipants.get(roomId) - 1);
                if (count > 0) {
                    roomParticipants.put(roomId, count);
                } else {
                    roomParticipants.remove(roomId);
                }
                logger.debug("룸 {} 참가자 수 업데이트: {}", roomId, count);
            }
            
            // offer/answer/ice 메시지 상세 로깅
            if ("offer".equals(type)) {
                logger.debug("Offer SDP 처리 (룸: {})", roomId);
            } else if ("answer".equals(type)) {
                logger.debug("Answer SDP 처리 (룸: {})", roomId);
            } else if ("ice".equals(type)) {
                logger.debug("ICE 후보 처리 (룸: {})", roomId);
            }
            
            // 방 ID별로 메시지 전송 (특정 방에만 전송)
            logger.debug("WebRTC 메시지 전달 (룸: {}): {}", roomId, type);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/rtc", message);
        } else {
            logger.warn("WebRTC 메시지에 룸 ID가 누락됨: {}", type);
        }
    }
} 