package kr.or.kosa.visang.domain.chat.service;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import kr.or.kosa.visang.domain.chat.repository.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final RedisTemplate<String, ChatMessage> redis;

    @Transactional
    public List<ChatMessage> getHistory(Long roomId) {
        return chatMapper.findByContractId(roomId);
    }

    public void saveMessageToRedis(ChatMessage msg) {
        String key = "chat:room:" + msg.getRoomId();
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
        String fileNmae = "chat_" + roomId + "_" + System.currentTimeMillis() + ".txt";

        //폴더 경로
        Path dir = Paths.get("data", "chats");

        //파일 경로
        Path path = dir.resolve(fileNmae);

        try {
            // 없으면 생성
            Files.createDirectories(dir);

            // 파일에 쓰기
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                for (ChatMessage msg : msgHistory) {
                    writer.write(String.format("[%s] %s: %s",
                            msg.getSendTime(),
                            msg.getSender(),
                            msg.getContent()));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
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
        // 1) Redis 기록 꺼내고
        List<ChatMessage> history = deleteFromRedis(roomId);

        // 2) 파일 생성
        String filePath = exportFile(roomId, history);

        // 3) END 메시지 INSERT → chatId 자동 채워짐
        ChatMessage endMsg = ChatMessage.builder()
                .roomId(roomId)
                .sender(username)
                .content("통화를 종료하고 이력을 파일로 저장했습니다.")
                .type("END")
                .build();
        chatMapper.insertMessage(endMsg);

        // 4) INSERT된 chat_id 로만 UPDATE
        Long newChatId = endMsg.getChatId();
        chatMapper.updateExportPathByChatId(Map.of(
                "chatId", newChatId,
                "filePath", filePath
        ));
    }


}

