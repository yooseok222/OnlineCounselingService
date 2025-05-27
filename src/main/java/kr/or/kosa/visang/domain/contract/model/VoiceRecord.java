package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceRecord {
    private Long voiceId;
    private String filePath;
    private Date createdAt;
    private Long contractId;
} 