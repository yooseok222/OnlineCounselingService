package kr.or.kosa.visang.domain.chat.repository;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMapper {

    void insertMessage(ChatMessage msg);

    String getChatExportFilePathByContractId(@Param("contractId") Long contractId);

    List<ChatMessage> findByContractId(Long roomId);

    void updateExportPath(Map<String, Object> params);

    String getExportPath(@Param("roomId") Long roomId);

    void updateExportPathByChatId(Map<String, Object> params);
}
