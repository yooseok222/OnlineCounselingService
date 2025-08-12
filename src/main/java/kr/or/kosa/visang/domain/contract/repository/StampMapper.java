package kr.or.kosa.visang.domain.contract.repository;

import kr.or.kosa.visang.domain.contract.model.StampDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StampMapper {
    // 도장 조회
    StampDTO selectStampById(Long stampId);
    
    // 고객 ID로 도장 목록 조회
    List<StampDTO> selectStampsByClientId(Long clientId);
    
    // 도장 추가
    int insertStamp(StampDTO stamp);
    
    // 도장 정보 업데이트
    int updateStamp(StampDTO stamp);
    
    // 도장 삭제
    int deleteStamp(Long stampId);
} 