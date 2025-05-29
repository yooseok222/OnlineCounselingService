package kr.or.kosa.visang.domain.chat.service;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import kr.or.kosa.visang.domain.chat.repository.ChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final RedisTemplate<String, ChatMessage> redis;
    
    @Value("${file.upload-dir.chat:./upload/chat}")
    private String chatUploadDir;

    @Transactional
    public List<ChatMessage> getHistory(Long roomId) {
        return chatMapper.findByContractId(roomId);
    }

    public void saveMessageToRedis(ChatMessage msg) {
        String key = "chat:room:" + msg.getContractId();
        redis.opsForList().rightPush(key, msg);
    }

    /* Redis에서 대화 이력 꺼내고 삭제하기*/
    public List<ChatMessage> deleteFromRedis(Long roomId) {
        String key = "chat:room:" + roomId;

        List<ChatMessage> msgs = redis.opsForList().range(key, 0, -1);
        redis.delete(key);
        return msgs != null ? msgs : Collections.emptyList();

    }

    /* 파일로 내보내고, 파일 경로 반환 */
    public String exportFile(Long roomId, List<ChatMessage> msgHistory) {
        // 현재 시간으로 파일명 생성
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        String fileName = "chat_" + roomId + "_" + timestamp + ".txt";

        // 설정된 절대경로 사용
        Path dir = Paths.get(chatUploadDir);
        Path path = dir.resolve(fileName);

        try {
            // 디렉토리가 없으면 생성
            Files.createDirectories(dir);
            log.info("채팅 파일 저장 경로: {}", path.toAbsolutePath());

            // 파일에 쓰기
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                // 헤더 정보 추가
                writer.write("=== 온라인 상담 채팅 기록 ===");
                writer.newLine();
                writer.write("계약 ID: " + roomId);
                writer.newLine();
                writer.write("생성 시간: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.newLine();
                writer.write("총 메시지 수: " + msgHistory.size());
                writer.newLine();
                writer.write("================================");
                writer.newLine();
                writer.newLine();

                // 채팅 메시지 작성
                for (ChatMessage msg : msgHistory) {
                    if (msg.getTimestamp() != null && msg.getSender() != null && msg.getContent() != null) {
                        String formattedTime = msg.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        String senderName = msg.getSenderName() != null ? msg.getSenderName() : 
                                           ("agent".equals(msg.getSender()) ? "상담원" : "고객");
                        
                        writer.write(String.format("[%s] %s: %s", formattedTime, senderName, msg.getContent()));
                        writer.newLine();
                    }
                }
                
                // 푸터 정보 추가
                writer.newLine();
                writer.write("=== 채팅 기록 끝 ===");
                
                log.info("채팅 파일 저장 완료: {} (메시지 수: {})", path.toAbsolutePath(), msgHistory.size());
            }
        } catch (IOException e) {
            log.error("채팅 기록 파일 생성 실패: {}", path.toAbsolutePath(), e);
            throw new RuntimeException("상담 기록 파일 생성 실패: " + path, e);
        }
        return path.toAbsolutePath().toString();
    }


    /* 다운로드 링크 만들 때 호출 */
    public String getExportPath(Long roomId) {
        return chatMapper.getExportPath(roomId);
    }

    @Transactional
    public void endAndExport(Long roomId, String username) {
        log.info("=== 채팅 기록 내보내기 시작 ===");
        log.info("계약 ID: {}", roomId);
        log.info("사용자: {}", username);
        log.info("설정된 채팅 저장 경로: {}", chatUploadDir);
        
        // 1) Redis 기록 꺼내고
        List<ChatMessage> history = deleteFromRedis(roomId);
        log.info("Redis에서 가져온 채팅 메시지 수: {}", history.size());
        
        if (history.isEmpty()) {
            log.warn("Redis에 저장된 채팅 기록이 없습니다. contractId: {}", roomId);
        } else {
            log.info("채팅 기록 샘플:");
            for (int i = 0; i < Math.min(3, history.size()); i++) {
                ChatMessage msg = history.get(i);
                log.info("  [{}] {}: {}", msg.getTimestamp(), msg.getSender(), 
                        msg.getContent() != null ? msg.getContent().substring(0, Math.min(50, msg.getContent().length())) : "null");
            }
        }

        // 2) 파일 생성
        String filePath = exportFile(roomId, history);
        log.info("생성된 파일 경로: {}", filePath);

        // 3) END 메시지 INSERT → chatId 자동 채워짐
        ChatMessage endMsg = ChatMessage.builder()
                .contractId(roomId)
                .sender(username)
                .content("통화를 종료하고 이력을 파일로 저장했습니다.")
                .type(ChatMessage.MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .build();
        chatMapper.insertMessage(endMsg);
        log.info("시스템 메시지 DB 저장 완료. 메시지 ID: {}", endMsg.getMessageId());

        // 4) INSERT된 chat_id 로만 UPDATE
        Long newChatId = endMsg.getMessageId();
        chatMapper.updateExportPathByChatId(Map.of(
                "chatId", newChatId,
                "filePath", filePath
        ));
        log.info("파일 경로 DB 업데이트 완료. 채팅 ID: {}, 파일 경로: {}", newChatId, filePath);
        log.info("=== 채팅 기록 내보내기 완료 ===");
    }


}

