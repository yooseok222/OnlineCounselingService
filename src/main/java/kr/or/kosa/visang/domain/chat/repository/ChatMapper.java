package kr.or.kosa.visang.domain.chat.repository;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMapper {

    void insertMessage(ChatMessage msg);

    List<ChatMessage> findByContractId(Long roomId);

    void updateExportPath(Map<String, Object> params);

    String getExportPath(Long roomId);
}
