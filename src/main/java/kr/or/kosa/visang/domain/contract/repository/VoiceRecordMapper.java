package kr.or.kosa.visang.domain.contract.repository;

import kr.or.kosa.visang.domain.contract.model.VoiceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoiceRecordMapper {
    
    /**
     * 녹음 파일 정보 저장
     */
    void insertVoiceRecord(VoiceRecord voiceRecord);
    
    /**
     * 계약 ID로 녹음 파일 목록 조회
     */
    List<VoiceRecord> findByContractId(@Param("contractId") Long contractId);
    
    /**
     * 녹음 파일 ID로 조회
     */
    VoiceRecord findById(@Param("voiceId") Long voiceId);
    
    /**
     * 녹음 파일 삭제
     */
    void deleteById(@Param("voiceId") Long voiceId);

    Long getVoiceIdByContractId(@Param("contractId") Long contractId);
} 