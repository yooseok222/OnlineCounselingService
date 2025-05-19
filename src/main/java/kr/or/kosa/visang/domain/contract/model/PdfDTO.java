package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

import java.util.Date;

@Data
public class PdfDTO {
    private Long pdfId;           // pdf_id
    private String filePath;      // file_path
    private String fileHash;      // file_hash
    private Date createdAt;       // created_at
    private Long contractId;      // contract_id
} 