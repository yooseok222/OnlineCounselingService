package kr.or.kosa.visang.domain.pdf.model;

import lombok.Data;

@Data
public class PDF {
    private Long pdfId; // PDF ID
    private String filePath; // 파일 경로
    private String fileHash; // 파일 해시값
    private String createdAt; // 생성 일시
    private Long contractId;
}
